#!/usr/bin/env bash
# Drives the app through a fixed screen tour on a booted emulator, taking an
# `adb exec-out screencap` after each checkpoint. Resolution-independent: it
# locates every tap target from a live `uiautomator dump` instead of hardcoded
# pixel coordinates, since the CI emulator's screen size isn't guaranteed to
# match any particular device.
#
# Required env vars: APP_PACKAGE, SCREENSHOT_DIR, CREDS_JSON
# CREDS_JSON must contain: customerEmail, customerPassword, adminEmail, adminPassword
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BOUNDS_PY="$SCRIPT_DIR/uiautomator_bounds.py"
DUMP_PATH="/sdcard/window_dump.xml"
LOCAL_DUMP="/tmp/window_dump.xml"

mkdir -p "$SCREENSHOT_DIR"

customer_email=$(jq -r .customerEmail "$CREDS_JSON")
customer_password=$(jq -r .customerPassword "$CREDS_JSON")
admin_email=$(jq -r .adminEmail "$CREDS_JSON")
admin_password=$(jq -r .adminPassword "$CREDS_JSON")

log() { echo "[ui-tour] $*"; }

dump_ui() {
  adb shell uiautomator dump "$DUMP_PATH" >/dev/null
  adb pull "$DUMP_PATH" "$LOCAL_DUMP" >/dev/null 2>&1
}

# find_bounds --id <resource-id> [--index N] | --text <text>  -> echoes "x y"
find_bounds() {
  dump_ui
  python3 "$BOUNDS_PY" "$LOCAL_DUMP" "$@"
}

# wait_for --id <resource-id> | --text <text> -- polls until the element appears
wait_for() {
  local tries=30
  for _ in $(seq 1 "$tries"); do
    if find_bounds "$@" >/tmp/bounds_out 2>/dev/null; then
      return 0
    fi
    sleep 1
  done
  log "TIMEOUT waiting for: $*"
  adb exec-out screencap -p >"$SCREENSHOT_DIR/ERROR-timeout-$(date +%s).png" || true
  return 1
}

# tap --id <resource-id> [--index N] | --text <text>
tap() {
  wait_for "$@"
  read -r x y <"/tmp/bounds_out"
  adb shell input tap "$x" "$y"
  sleep 1
}

type_text() {
  # `adb shell input text` needs spaces escaped and can't send some symbols;
  # good enough for the plain test data this tour uses.
  adb shell input text "${1// /%s}"
  sleep 0.3
}

back() {
  adb shell input keyevent 4
  sleep 1
}

screenshot() {
  local name="$1"
  sleep 1
  adb exec-out screencap -p >"$SCREENSHOT_DIR/$name.png"
  log "captured $name.png"
}

start_activity() {
  adb shell am start -n "$APP_PACKAGE/$1" >/dev/null
  sleep 2
}

log "Waking device and dismissing keyguard"
adb shell input keyevent 224 >/dev/null || true # KEYCODE_WAKEUP
adb shell wm dismiss-keyguard >/dev/null 2>&1 || true

log "Installing APK"
adb install -r -t "$APK_PATH"

log "Launching app"
start_activity ".LoginActivity"

log "Step: log in as customer"
tap --id editTextUserName
type_text "$customer_email"
back # dismiss keyboard before switching fields, keeps the next tap's coordinates stable
tap --id editTextPassword
type_text "$customer_password"
back
tap --id loginButton

log "Step: Menu screen"
wait_for --id menuRecyclerView
wait_for --id addToCartButton --index 1
screenshot "01-menu"

log "Step: add two items to cart"
tap --id addToCartButton --index 0
tap --id addToCartButton --index 1

log "Step: Cart screen"
tap --id cartButton
wait_for --id cartRecyclerView
screenshot "02-cart"

log "Step: Ask AI chat, mid-conversation"
tap --id askAiFab
wait_for --id chatInput
tap --id chatInput
type_text "What do you recommend?"
tap --id chatSendButton
# Wait for the assistant's reply: the typing indicator (chatTypingText) disappears
# once a reply lands, so poll for it to go away instead of a fixed sleep.
for _ in $(seq 1 30); do
  dump_ui
  if ! grep -q 'chatTypingText' "$LOCAL_DUMP" 2>/dev/null; then
    break
  fi
  sleep 1
done
sleep 1
screenshot "03-ai-chat"
back # dismiss keyboard/bottom sheet
back || true

log "Step: place an order"
wait_for --id placeOrderButton
tap --id placeOrderButton
wait_for --id cardPaymentOption
tap --id cardPaymentOption
tap --id cardNumberInput
type_text "4242424242424242"
back
tap --id cardholderNameInput
type_text "TestUser"
back
tap --id expiryDateInput
type_text "1230"
back
tap --id cvvInput
type_text "123"
back
tap --id payNowButton

log "Step: Order confirmation, live status"
wait_for --id orderStatusBadge
screenshot "04-order-confirmation"

log "Step: Admin dashboard"
start_activity ".AdminLoginActivity"
tap --id adminEmailInput
type_text "$admin_email"
back
tap --id adminPasswordInput
type_text "$admin_password"
back
tap --id adminLoginButton
wait_for --id adminWelcomeText
screenshot "05-admin-dashboard"

log "Tour complete. Screenshots in $SCREENSHOT_DIR:"
ls -la "$SCREENSHOT_DIR"
