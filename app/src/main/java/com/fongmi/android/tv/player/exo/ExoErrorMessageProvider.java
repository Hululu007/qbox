package com.fongmi.android.tv.player.exo;

import androidx.media3.common.PlaybackException;

/**
 * 播放错误文案统一为中文。
 *
 * <p>AndroidX Media3 的默认/原始错误名是英文（如 Network Connection Failed），
 * 混在纯中文界面里非常突兀；这里按错误码给出一句人话，detail 页与直播页共用。
 */
public class ExoErrorMessageProvider {

    public String get(PlaybackException e) {
        return switch (e.errorCode) {
            case PlaybackException.ERROR_CODE_TIMEOUT -> "连接超时";
            case PlaybackException.ERROR_CODE_UNSPECIFIED -> "播放失败";
            case PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK -> "运行环境异常";
            case PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> "网络错误";
            case PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "服务器返回错误";
            case PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> "内容格式不支持";
            case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "网络连接失败";
            case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "网络连接超时";
            case PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE -> "读取位置越界";
            case PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "播放列表损坏";
            case PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "媒体容器损坏";
            case PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED -> "播放列表不支持";
            case PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "媒体格式不支持";
            case PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "解码器初始化失败";
            case PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> "解码器查询失败";
            case PlaybackException.ERROR_CODE_DECODING_FAILED -> "解码失败";
            case PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> "解码格式不支持";
            case PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED -> "解码资源被回收";
            case PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES -> "设备解码能力不足";
            case PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED -> "音频初始化失败";
            case PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED -> "音频写入失败";
            case PlaybackException.ERROR_CODE_DRM_UNSPECIFIED -> "版权保护错误";
            case PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR -> "版权保护系统错误";
            case PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR -> "版权保护内容错误";
            case PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED -> "设备版权授权失效";
            case PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED -> "版权授权已过期";
            case PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED -> "版权授权配置失败";
            case PlaybackException.ERROR_CODE_DRM_DISALLOWED_OPERATION -> "版权保护不允许该操作";
            case PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED -> "版权授权获取失败";
            default -> "播放失败";
        };
    }
}
