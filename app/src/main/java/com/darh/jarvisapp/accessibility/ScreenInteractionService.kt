package com.darh.jarvisapp.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.darh.jarvisapp.api.OPEN_AI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class ScreenInteractionService : AccessibilityService() {

    companion object {
        private var instance: ScreenInteractionService? = null

        fun getInstance(): ScreenInteractionService? {
            return instance
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow
        rootNode?.let {
            processNode(it)
        }
    }

    override fun onInterrupt() {
        // Handle interruptions
    }

    private val elementMap = mutableMapOf<AccessibilityNodeInfo, ElementData>()

    private fun processNode(node: AccessibilityNodeInfo?) {
        if (node == null) return

        // Read content and store the element data
        val text = node.text
        val location = Rect()
        node.getBoundsInScreen(location)
        val id = node.viewIdResourceName
        elementMap[node] = ElementData(id, text, location)

        // Recursively process child nodes
        for (i in 0 until node.childCount) {
            processNode(node.getChild(i))
        }
    }

    data class ElementData(val id: String?, val text: CharSequence?, val location: Rect)

    fun clickNode(node: AccessibilityNodeInfo) {
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun swipeNode(node: AccessibilityNodeInfo, direction: Int) {
        if (direction > 0) {
            node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        } else {
            node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
        }
    }

    fun getElementMap(): Map<AccessibilityNodeInfo, ElementData> {
        return elementMap
    }

    fun mapElementsToString() {
        GlobalScope.launch {
            delay(2000L)
            val stringBuilder = StringBuilder()
            elementMap.forEach { (_, elementData) ->
                val id = elementData.id?.split(":")?.lastOrNull()?.trim() ?: "N/A"
                val text = elementData.text?.toString()?.trim() ?: "N/A"
                stringBuilder.append("ID: $id, Text: '$text', Location: ${elementData.location}\n")
            }
            stringBuilder.toString().let {
                Timber.tag(OPEN_AI).i("Screen description!!!!!!!!\n$it")
            } ?: Timber.tag(OPEN_AI).w("Screen description failed, service not active.")

            elementMap.forEach { entry ->
                entry.value.text?.takeIf { it.contains("Nemo", true) }?.let {
                    clickNode(entry.key)
                }
            }
        }
    }
}
