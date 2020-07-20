package com.aziz.spark.utils

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.gson.Gson
import com.google.gson.JsonArray
import okhttp3.MediaType
import androidx.recyclerview.widget.RecyclerView

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.anko.AlertBuilder
import org.jetbrains.anko.alert
import org.jetbrains.anko.okButton
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.Serializable
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**Created by Aziz*/

fun Spinner.setSampleSpinnerAdapter(optionalList: List<String>? = null){
    this.adapter = this.context.sampleArrayAdapter(optionalList)
}

fun Context.sampleArrayAdapter(optionalList: List<String>?) = ArrayAdapter(this, android.R.layout.simple_list_item_1, optionalList ?: emptyList())

 fun getSampleList(prefix:String = "Item - ", count:Int = 5): List<String> {
    val list:MutableList<String> = ArrayList()
    for(i in 0 until count)
        list.add("$prefix $i")

    return list
}

fun getFormattedDate(timeInMillis:Long = System.currentTimeMillis(), pattern:String = "yyyy-MM-dd"):String{
    return SimpleDateFormat(pattern, Locale.getDefault())
            .format(Date(timeInMillis))
}


fun FragmentManager?.loadFragment(
        @IdRes containerID: Int, fragment: Fragment,
        shouldRemovePreviousFragments: Boolean = true, currentTitle: CharSequence? = null
){
    val transaction = this?.beginTransaction()


    if(shouldRemovePreviousFragments) {
        repeat(this?.backStackEntryCount?:0) {
            this?.popBackStack()
        }
    }


    transaction?.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)

    transaction?.addToBackStack(null)
            ?.replace(containerID, fragment, currentTitle?.toString())
            ?.commit()


}


fun Fragment.addArgs(vararg pairs: Pair<String,Any?>) = this.apply {

    arguments = Bundle().apply {
        pairs.forEach { pair ->
            when (pair.second) {
                is Int -> putInt(pair.first, pair.second as Int)
                is String -> putString(pair.first, pair.second.toString())
                is Serializable -> putSerializable(pair.first, pair.second as Serializable)
                is Boolean -> putBoolean(pair.first, pair.second as Boolean)
                else -> Log.e("FragmentUtils", "addArgs: Pair not available, Please add it")
            }


        }
    }
}



fun Intent.addArgs(vararg pairs: Pair<String,Any?>) = this.apply {

    this.apply {
        pairs.forEach { pair ->
            when (pair.second) {
                is Int -> putExtra(pair.first, pair.second as Int)
                is String -> putExtra(pair.first, pair.second.toString())
                is Serializable -> putExtra(pair.first, pair.second as Serializable)
                is Boolean -> putExtra(pair.first, pair.second as Boolean)
                else -> Log.e("Intent", "addArgs: Pair not available, Please add it")
            }


        }
    }
}

fun Intent.optString(key:String, optionalVal:String = "") = kotlin.run {
    if(hasExtra(key))
        getStringExtra(key)
    else
        optionalVal
}


val EditText.getText:String
    get() = this.text.toString()



val Context.hasLocationPermission: Boolean
    get() = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

val Context.hasStoragePermission: Boolean
    get() = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED


 fun RecyclerView.setCheckboxAdapter(
        jsonArray: JSONArray,
        initialCheckList: List<String>?,
        jsonObjectKey: String = "name", initialCheckListField:String = "id",
        onCheckedSkills: (checkedItems: ArrayList<JSONObject>, uncheckedItems: ArrayList<JSONObject>) -> Unit
){


    val checkedItems:ArrayList<JSONObject> = ArrayList()
    val uncheckedItems:ArrayList<JSONObject> = ArrayList()


    adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val checkBox = CheckBox(parent.context)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(30,10,10,10)
            checkBox.layoutParams = params

            return object : RecyclerView.ViewHolder(checkBox){}
        }

        override fun getItemCount(): Int = jsonArray.length()

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            val checkbox = holder.itemView as? CheckBox
            val jsonObject = jsonArray.getJSONObject(position)
            checkbox?.let {
                val name =  jsonObject.optString(jsonObjectKey)
                checkbox.text = name

                checkbox.isChecked = initialCheckList?.any{ it == jsonObject.optString(initialCheckListField,"nil") }?:false

                if(checkbox.isChecked) {
                    checkedItems.add(jsonObject)
                    uncheckedItems.remove(jsonObject)
                }
                else {
                    checkedItems.remove(jsonObject)
                    uncheckedItems.add(jsonObject)
                }


                onCheckedSkills.invoke(checkedItems,uncheckedItems)


                it.setOnCheckedChangeListener { _, isChecked ->

                    if(isChecked) {
                        checkedItems.add(jsonObject)
                        uncheckedItems.remove(jsonObject)
                    }
                    else {
                        checkedItems.remove(jsonObject)
                        uncheckedItems.add(jsonObject)
                    }

                    onCheckedSkills.invoke(checkedItems,uncheckedItems)
                }
            }

        }

    }
}


 fun RecyclerView.setCheckboxAdapter(
        items: List<String>,
        initialCheckList: List<String>?,
        onCheckedSkills: ((checkedItems: ArrayList<String>, uncheckedItems: ArrayList<String>) -> Unit?)? = null
){


    val checkedItems:ArrayList<String> = ArrayList()
    val uncheckedItems:ArrayList<String> = ArrayList()


    adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val checkBox = CheckBox(parent.context)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(30,10,10,10)
            checkBox.layoutParams = params

            return object : RecyclerView.ViewHolder(checkBox){}
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            val checkbox = holder.itemView as? CheckBox
            val item = items[position]

            checkbox?.text = item
            checkbox?.let {

                checkbox.isChecked = initialCheckList?.any{ it == item }?:false

                if(checkbox.isChecked) {
                    checkedItems.add(item)
                    uncheckedItems.remove(item)
                }
                else {
                    checkedItems.remove(item)
                    uncheckedItems.add(item)
                }


                onCheckedSkills?.invoke(checkedItems,uncheckedItems)


                it.setOnCheckedChangeListener { _, _ ->

                    if(checkbox.isChecked) {
                        checkedItems.add(item)
                        uncheckedItems.remove(item)
                    }
                    else {
                        checkedItems.remove(item)
                        uncheckedItems.add(item)
                    }

                    onCheckedSkills?.invoke(checkedItems,uncheckedItems)
                }
            }

        }

    }
}



 fun Context.showProgressDialog(shouldShowInitially:Boolean = true): ProgressDialog {
    val dialog = ProgressDialog(this)
    with(dialog){
        setCancelable(false)
        setMessage("Please wait...")
        if(shouldShowInitially)
            show()
    }

    return dialog
}




 fun isEditTextValid(vararg editText: EditText):Boolean{
    var isValid = true
    editText.forEach {
        if(it.text.isEmpty()){
            isValid = false
            it.error = "Field required"
        }
    }

    editText.getOrNull(0)?.requestFocus()

    return isValid
}


  fun Context.hideKeyboard(){
    val context = this
    val activity = context as Activity
    val windowToken = activity.window.decorView.rootView.windowToken
    val inputService = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputService.hideSoftInputFromWindow(windowToken , 0)

}



 fun Context.showInfoDialog(dialogMessage:String, cancelable:Boolean = true, onOkClick: (() -> Unit?)? = null): AlertBuilder<DialogInterface> {
    val alert = alert { message = dialogMessage
//        iconResource = R.drawable.ic_info_outline_black_24dp
//        title = "Info"
        okButton { onOkClick?.let { it1 -> it1() } }
        isCancelable = cancelable
    }
    alert.show()
    return alert
}

 fun Context.showConfirmDialog(dialogMessage:String, onOkClick: (() -> Unit?)?): AlertBuilder<DialogInterface> {
    val alert = alert { message = dialogMessage
//        iconResource = R.drawable.ic_info_outline_black_24dp
//        title = "Confirm"
        positiveButton("Yes") { onOkClick?.let { it1 -> it1() } }
        negativeButton("No") {  }
    }
    alert.show()
    return alert
}



 fun String.parseDate(pattern:String = "yyyy-MM-dd") = kotlin.run {
    try{
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.parse(this)
    }
    catch (e:Exception){
        null
    }
}




inline fun View.hide(){
    visibility = View.GONE
}

inline fun View.show(){
    visibility = View.VISIBLE
}
var View.isVisible
get() = visibility == View.VISIBLE
set(value) {
    if(value == true)
        visibility = View.VISIBLE
    else
        visibility = View.GONE
}

fun View.hideOrShow(){
    if (visibility == View.VISIBLE) hide()
    else show()
}

fun String.toIntWithDefault(default:Int = 0) = try { this.toInt() } catch (e:Exception){ default }

fun String.ignoreNullValue(replacement:String = "") = if(this == "null" || this.isEmpty()) replacement else this

inline val Double?.ignoreNullValue get() = if (this?.isNaN() == true) 0.0 else this

inline val Double.roundOneDecimal get() = String.format("%.1f", this)
inline val Double.roundTwoDecimal get() = String.format("%.2f", this)
inline val Double.roundTwoDecimalString get() = String.format("%.2f", this)
inline val Double.roundtoTwoPlace get() = (String.format("%.2f", this).toDoubleOrNull())?:0.0

fun Double.formatToDollarString(requireDollar:Boolean = true): String? = kotlin.run {
    val symbols = DecimalFormatSymbols()
    symbols.groupingSeparator = ','
    symbols.decimalSeparator = '.'

    val currency = if(requireDollar) "$ " else ""
    val formatter = "$currency#,##0.00"
    val decimalFormat = DecimalFormat(formatter, symbols)
    decimalFormat.format(this)?:"$currency 0.0"

}

val String.getDoubleFromString: String
    get() = run {
        val regex = "[$,]".toRegex()
        this.replace(regex, "").trim()
    }


fun Double.percentOf(of:Double) = kotlin.run{
    (this.div(100) * of).roundtoTwoPlace
}




fun TextView.bindEditTextToDatePicker(context: Context, year:Int = Calendar.getInstance().get(Calendar.YEAR),
                                             month:Int = Calendar.getInstance().get(Calendar.MONTH),
                                             dayOfMonth:Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)){
    isFocusable = false


    val dialog = DatePickerDialog(context,
            DatePickerDialog.OnDateSetListener { p, yyyy, m, d ->

                val mm = String.format("%2d",m+1)
                val date = "$d/$mm/$yyyy"
                this.text = date

            }, year, month, dayOfMonth)

    setOnClickListener {
        dialog.show()
    }

}



inline fun RecyclerView.setJSONArrayAdapter(context: Context, jsonArray: JSONArray, resID:Int ,
                                            crossinline onBindViewHolder: (itemView: View, position: Int, jsonObject:JSONObject) -> Unit)
        :RecyclerView.Adapter<RecyclerView.ViewHolder>{

    val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            val view = LayoutInflater.from(context).inflate(resID, parent, false)
            return object : RecyclerView.ViewHolder(view){}
        }

        override fun getItemCount(): Int = jsonArray.length()

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            onBindViewHolder.invoke(holder.itemView, position, jsonArray.getJSONObject(position))
        }

    }


    this.adapter = adapter

    return adapter

}


fun areAnyNull(vararg any:Any?) = any.any { it == null }



fun <T> RecyclerView.setCustomAdapter(
        items: Collection<T>, @LayoutRes resID: Int,
        onBindViewHolder: (itemView: View, position: Int, item: T) -> Unit
):RecyclerView.Adapter<RecyclerView.ViewHolder>{

    val adapter =  object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            val view = LayoutInflater.from(this@setCustomAdapter.context).inflate(resID, parent, false)
            return object : RecyclerView.ViewHolder(view){}
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            onBindViewHolder.invoke(holder.itemView, position, items.elementAt(position))
        }

    }
    this.adapter = adapter
    return adapter


}


fun <T> RecyclerView.setCustomAdapter(
        items: Collection<T>,
        view: View,
        onBindViewHolder: (itemView: View, position: Int, item: T) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder> {

    val adapter =  object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            return object : RecyclerView.ViewHolder(view){}
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            onBindViewHolder.invoke(holder.itemView, position, items.elementAt(position))
        }

    }
    this.adapter = adapter
    return adapter

}

val Int.toDoubleDigitString
    get() = this.run {
        if (this > 9) this.toString()
        else "0$this"
    }

fun TextView.setToAmountMode(){

    val field = this

    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun afterTextChanged(s: Editable?) {

        }

        @SuppressLint("SetTextI18n")
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (s.toString().isEmpty()) return
            field.removeTextChangedListener(this)
            val cleanString = s.toString().replace("[$,.]".toRegex(), "")
            val parsed = BigDecimal(cleanString).setScale(2, BigDecimal.ROUND_FLOOR)
                    .divide(BigDecimal(100), BigDecimal.ROUND_FLOOR)
//                val formatted = NumberFormat.getCurrencyInstance().format(parsed)

            field.text = parsed.toString()
            field.addTextChangedListener(this)
        }
    })
}


fun Activity.getPathFromURI(contentUri: Uri?): String? {

    if(contentUri == null) return ""

    var res: String? = null
    val proj = arrayOf(MediaStore.Images.Media.DATA)
    val cursor =
            contentResolver.query(contentUri, proj, "", null, "")
    while (cursor != null && cursor.moveToNext()) {
        val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        res = cursor.getString(column_index)
    }
    cursor?.close()
    return res
}

val Float.toDoubleDigitDecimalString
    get() = String.format("%.2f", this)

fun Context.getFileFromBitmap(bitmap: Bitmap): File? {
    val imageFile = File(filesDir, "Image_${System.currentTimeMillis()}_" + ".jpg")

    val os: OutputStream
    try {
        os = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
        os.flush()
        os.close()
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }

    return imageFile
}


fun TextView.animateDollarValue(finalValue:Double){

    animateValue(finalValue, true){
        text =  it.formatToDollarString() //context.getString(R.string.format_dollar, it)
    }

}

fun TextView.animateValue(finalValue:Double, isDollar:Boolean = false, onUpdate: ((value: Double) -> Unit)? = null){
    ValueAnimator.ofInt(0, 100).apply {
        duration = 1000
        addUpdateListener {
            val value = (it.animatedValue.toString().toInt() * finalValue).div(100)
            text = if(!isDollar)
                value.toInt().toString()
            else
                value.roundtoTwoPlace.toString()
            onUpdate?.invoke(value)
        }
    }.start()
}

fun TextView.addStrikeThru(isGray:Boolean = true) = apply {
    paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    if(isGray) setTextColor(Color.GRAY)
}

fun TextView.removeStrikeThru(isBlack:Boolean = true) = apply {
    paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
    if(isBlack) setTextColor(Color.BLACK)
}


val String.ignoreEmptyToNA
    get() = this.takeUnless { it.isNotEmpty() }?:"N/A"



