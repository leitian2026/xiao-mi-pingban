package com.hyperfocus.api

import android.graphics.drawable.Icon
import android.os.Bundle
import android.widget.RemoteViews

/**
 * HyperFocusApi（com.github.ghhccghk:HyperFocusApi:1.3）的本地占位实现。
 *
 * 原依赖在 JitPack 上已无法下载（1.3 版本不存在），而 HyperCeiler 只在 MusicBaseHook 中使用它，
 * 用来给歌词通知附加小米"焦点通知"的扩展数据。这里提供同名、同参数名的接口，保证编译通过；
 * 返回空 Bundle，歌词会以普通常驻通知显示（标题、图标等已在 MusicBaseHook 中设置），
 * 不再有焦点通知（岛 / 锁屏焦点）的样式。
 */
object FocusApi {

    class BaseInfo(
        val basetype: Int = 1,
        val title: String? = null
    )

    fun baseinfo(
        basetype: Int = 1,
        title: String? = null
    ): BaseInfo = BaseInfo(basetype, title)

    @Suppress("UNUSED_PARAMETER")
    fun senddiyFocus(
        ticker: String? = null,
        updatable: Boolean = false,
        aodTitle: String? = null,
        aodPic: Icon? = null,
        enableFloat: Boolean = true,
        rv: RemoteViews? = null,
        rvNight: RemoteViews? = null,
        timeout: Int = 0,
        picticker: Icon? = null
    ): Bundle = Bundle()

    @Suppress("UNUSED_PARAMETER")
    fun sendFocus(
        ticker: String? = null,
        updatable: Boolean = false,
        aodTitle: String? = null,
        aodPic: Icon? = null,
        enableFloat: Boolean = true,
        baseInfo: BaseInfo? = null,
        timeout: Int = 0,
        picticker: Icon? = null
    ): Bundle = Bundle()
}
