package org.bitanon.studfinder

/**
 * Created by rob on 2016-07-10.
 */
interface ParentListenerInterface {
	fun alert(str: String?)
	fun updateUi()
	fun setWaitScreen(boo: Boolean)
}