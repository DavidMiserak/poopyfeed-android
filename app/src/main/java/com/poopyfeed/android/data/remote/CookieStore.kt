package com.poopyfeed.android.data.remote

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CookieStore
    @Inject
    constructor() : CookieJar {
        private val cookies = mutableMapOf<String, MutableList<Cookie>>()

        override fun saveFromResponse(
            url: HttpUrl,
            cookies: List<Cookie>,
        ) {
            val host = url.host
            this.cookies.getOrPut(host) { mutableListOf() }.apply {
                // Remove existing cookies with the same name before adding new ones
                cookies.forEach { newCookie ->
                    removeAll { it.name == newCookie.name }
                }
                addAll(cookies)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookies[url.host]?.filter { !it.expiresAt.let { exp -> exp < System.currentTimeMillis() } }
                ?: emptyList()
        }

        fun clear() {
            cookies.clear()
        }
    }
