package net.poopyfeed.pf.ui.toast

import javax.inject.Inject

class TestToastManager @Inject constructor() : ToastManager {
    private val _messages = mutableListOf<Pair<String, MessageType>>()

    val messages: List<Pair<String, MessageType>> get() = _messages.toList()

    enum class MessageType {
        SUCCESS, ERROR, INFO
    }

    override fun showSuccess(message: String) {
        _messages.add(message to MessageType.SUCCESS)
    }

    override fun showError(message: String) {
        _messages.add(message to MessageType.ERROR)
    }

    override fun showInfo(message: String) {
        _messages.add(message to MessageType.INFO)
    }

    // Assertion helpers
    fun assertSuccess(expectedMessage: String) {
        val success = messages.firstOrNull { (msg, type) ->
            msg == expectedMessage && type == MessageType.SUCCESS
        }
        check(success != null) {
            "Expected success message '$expectedMessage' but got: ${messages.map { it.first }}"
        }
    }

    fun assertError(expectedMessage: String? = null) {
        if (expectedMessage == null) {
            val hasError = messages.any { it.second == MessageType.ERROR }
            check(hasError) {
                "Expected an error message but got: ${messages.map { it.first }}"
            }
        } else {
            val error = messages.firstOrNull { (msg, type) ->
                msg == expectedMessage && type == MessageType.ERROR
            }
            check(error != null) {
                "Expected error message '$expectedMessage' but got: ${messages.map { it.first }}"
            }
        }
    }

    fun assertInfo(expectedMessage: String) {
        val info = messages.firstOrNull { (msg, type) ->
            msg == expectedMessage && type == MessageType.INFO
        }
        check(info != null) {
            "Expected info message '$expectedMessage' but got: ${messages.map { it.first }}"
        }
    }

    fun clear() {
        _messages.clear()
    }
}
