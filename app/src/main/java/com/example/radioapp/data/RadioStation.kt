package com.example.radioapp.data

import androidx.compose.runtime.Immutable

@Immutable
data class RadioStation(
    val id: String,
    val name: String,
    val url: String,
    val isBuiltIn: Boolean = true
)

object DefaultStations {
    val stations = listOf(
        RadioStation("1", "央广中国之声", "http://ngcdn001.cnr.cn/live/zgzs/index.m3u8"),
        RadioStation("2", "央广经济之声", "http://ngcdn001.cnr.cn/live/jjzs/index.m3u8"),
        RadioStation("3", "央广音乐之声", "http://ngcdn001.cnr.cn/live/yyzs/index.m3u8"),
        RadioStation("4", "北京交通广播", "http://ngcdn001.cnr.cn/live/bj_jt/index.m3u8"),
        RadioStation("5", "上海东方广播", "http://ngcdn001.cnr.cn/live/sh_df/index.m3u8"),
        RadioStation("6", "广东珠江经济台", "http://ngcdn001.cnr.cn/live/gd_zj/index.m3u8"),
        RadioStation("7", "深圳音乐广播", "http://ngcdn001.cnr.cn/live/sz_yy/index.m3u8"),
        RadioStation("8", "浙江交通广播", "http://ngcdn001.cnr.cn/live/zj_jt/index.m3u8"),
        RadioStation("9", "江苏新闻广播", "http://ngcdn001.cnr.cn/live/js_xw/index.m3u8"),
        RadioStation("10", "山东广播新闻频道", "http://ngcdn001.cnr.cn/live/sd_xw/index.m3u8")
    )
}
