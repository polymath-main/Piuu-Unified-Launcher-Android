package com.piuu.launcher.model

import com.google.gson.annotations.SerializedName

data class GridPosition(
    @SerializedName("row") val row: Int = 0,
    @SerializedName("col") val col: Int = 0,
    @SerializedName("spanX") val spanX: Int = 1,
    @SerializedName("spanY") val spanY: Int = 1
)

data class ElementStyle(
    @SerializedName("padding") val padding: Int = 12,
    @SerializedName("borderRadius") val borderRadius: Int = 16,
    @SerializedName("backgroundColor") val backgroundColor: String = "#1E1E24",
    @SerializedName("alpha") val alpha: Float = 0.95f,
    @SerializedName("borderWidth") val borderWidth: Float = 1.5f,
    @SerializedName("borderColor") val borderColor: String = "#A239CA"
)

data class IconProperties(
    @SerializedName("size") val size: Int = 64,
    @SerializedName("maskShape") val maskShape: String = "SQUIRCLE",
    @SerializedName("customIconSource") val customIconSource: String? = null,
    @SerializedName("showLabel") val showLabel: Boolean = true,
    @SerializedName("labelColor") val labelColor: String = "#FFFFFF"
)

data class ElementAction(
    @SerializedName("type") val type: String = "LAUNCH_APP",
    @SerializedName("target") val target: String = ""
)

data class DataBinding(
    @SerializedName("source") val source: String = "",
    @SerializedName("refreshInterval") val refreshInterval: Int = 5000,
    @SerializedName("templateJson") val templateJson: String = ""
)

data class AtomicElement(
    @SerializedName("elementId") val elementId: String,
    @SerializedName("type") val type: String,
    @SerializedName("gridPosition") val gridPosition: GridPosition = GridPosition(),
    @SerializedName("style") val style: ElementStyle = ElementStyle(),
    @SerializedName("iconProperties") val iconProperties: IconProperties = IconProperties(),
    @SerializedName("action") val action: ElementAction = ElementAction(),
    @SerializedName("dataBinding") val dataBinding: DataBinding = DataBinding()
)
