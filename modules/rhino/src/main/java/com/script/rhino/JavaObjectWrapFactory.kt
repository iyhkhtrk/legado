package com.script.rhino

import org.mozilla.javascript.lc.type.TypeInfo
import org.mozilla.javascript.Scriptable

fun interface JavaObjectWrapFactory {

    fun wrap(scope: Scriptable?, javaObject: Any, staticType: TypeInfo?): Scriptable

}
