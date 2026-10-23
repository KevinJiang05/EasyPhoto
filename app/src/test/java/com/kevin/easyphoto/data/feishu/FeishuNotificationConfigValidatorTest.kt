package com.kevin.easyphoto.data.feishu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeishuNotificationConfigValidatorTest {
    @Test
    fun validCustomBotConfig_isAccepted() {
        val result = FeishuNotificationConfigValidator.validate(
            FeishuNotificationConfig(
                appId = "cli_example",
                appSecret = "test-only-not-a-real-secret",
                webhookUrl = "https://open.feishu.cn/open-apis/bot/v2/hook/example",
            ),
        )

        assertNull(result)
    }

    @Test
    fun nonFeishuWebhook_isRejected() {
        val result = FeishuNotificationConfigValidator.validate(
            FeishuNotificationConfig(
                appId = "cli_example",
                appSecret = "test-only-not-a-real-secret",
                webhookUrl = "https://example.com/hook",
            ),
        )

        assertEquals("Webhook 必须是飞书 HTTPS 地址", result)
    }

    @Test
    fun blankCredential_isRejectedBeforeNetworkRequest() {
        val result = FeishuNotificationConfigValidator.validate(
            FeishuNotificationConfig(
                appId = "",
                appSecret = "test-only-not-a-real-secret",
                webhookUrl = "https://open.feishu.cn/open-apis/bot/v2/hook/example",
            ),
        )

        assertEquals("请填写飞书 App ID", result)
    }
}
