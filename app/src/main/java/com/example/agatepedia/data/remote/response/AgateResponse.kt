package com.example.agatepedia.data.remote.response

import com.google.gson.annotations.SerializedName

data class AgateResponse(

	@field:SerializedName("AgateResponse")
	val agateResponse: List<AgateResponseItem?>? = null
)

data class AgateResponseItem(

	@field:SerializedName("harga")
	val harga: String? = null,

	@field:SerializedName("penjelasan")
	val penjelasan: String? = null,

	@field:SerializedName("jenis")
	val jenis: String? = null,

	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("gambar")
	val gambar: String? = null
)
