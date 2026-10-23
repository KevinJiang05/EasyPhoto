package com.kevin.easyphoto.data.feishu

import java.net.URI

data class FeishuNotificationConfig(
    val appId: String,
    val appSecret: String,
    val webhookUrl: String,
)

object FeishuNotificationConfigValidator {
    fun validate(config: FeishuNotificationConfig): String? {
        if (config.appId.isBlank()) return "请填写飞书 App ID"
        if (config.appSecret.isBlank()) return "请填写飞书 App Secret"
        if (config.webhookUrl.isBlank()) return "请填写飞书 Webhook 地址"

        val uri = runCatching { URI(config.webhookUrl.trim()) }.getOrNull()
            ?: return "Webhook 地址格式不正确"
        if (uri.scheme != "https" || uri.host != "open.feishu.cn") {
            return "Webhook 必须是飞书 HTTPS 地址"
        }
        if (!uri.path.orEmpty().startsWith("/open-apis/bot/v2/hook/")) {
            return "请使用飞书群自定义机器人的 Webhook 地址"
        }
        return null
    }
}
