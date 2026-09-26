package com.example.cafeshopassignment.di

import com.example.cafeshopassignment.BuildConfig
import com.example.cafeshopassignment.data.remote.CafeApi
import com.example.cafeshopassignment.data.remote.FirebaseIdTokenProvider
import com.example.cafeshopassignment.data.remote.RetrofitClient
import com.example.cafeshopassignment.data.repository.AuthRepository
import com.example.cafeshopassignment.data.repository.CartRepository
import com.example.cafeshopassignment.data.repository.ChatRepository
import com.example.cafeshopassignment.data.repository.FirebaseAuthRepository
import com.example.cafeshopassignment.data.repository.FirestoreMenuRepository
import com.example.cafeshopassignment.data.repository.FirestoreNotificationRepository
import com.example.cafeshopassignment.data.repository.FirestoreOrderRepository
import com.example.cafeshopassignment.data.repository.FirestoreReviewRepository
import com.example.cafeshopassignment.data.repository.FirestoreUserRepository
import com.example.cafeshopassignment.data.repository.MenuRepository
import com.example.cafeshopassignment.data.repository.NotificationRepository
import com.example.cafeshopassignment.data.repository.OrderRepository
import com.example.cafeshopassignment.data.repository.RemoteChatRepository
import com.example.cafeshopassignment.data.repository.ReviewRepository
import com.example.cafeshopassignment.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Minimal manual dependency injection: app-wide singletons created lazily on first use.
 * ViewModel factories pull their dependencies from here; unit tests construct ViewModels
 * directly with fakes/mocks instead.
 */
object ServiceLocator {
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val cartRepository: CartRepository by lazy { CartRepository() }

    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(firebaseAuth, firestore, cartRepository)
    }

    val userRepository: UserRepository by lazy { FirestoreUserRepository(firestore) }
    val menuRepository: MenuRepository by lazy { FirestoreMenuRepository(firestore) }
    val orderRepository: OrderRepository by lazy { FirestoreOrderRepository(firestore) }
    val reviewRepository: ReviewRepository by lazy { FirestoreReviewRepository(firestore) }
    val notificationRepository: NotificationRepository by lazy { FirestoreNotificationRepository(firestore) }

    /** Backend client; the base URL comes from BACKEND_BASE_URL (local.properties / Gradle property). */
    private val cafeApi: CafeApi by lazy {
        RetrofitClient.create(
            baseUrl = BuildConfig.BACKEND_BASE_URL,
            tokenProvider = FirebaseIdTokenProvider(firebaseAuth),
            enableLogging = BuildConfig.DEBUG,
        )
    }

    val chatRepository: ChatRepository by lazy {
        RemoteChatRepository(cafeApi) { authRepository.currentUserId }
    }
}
