package com.indybrain.indypos_Android.presentation.discount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class DiscountViewModel @Inject constructor() : ViewModel() {
    
    private val _discount = MutableStateFlow(
        DiscountModel(
            type = DiscountType.PERCENTAGE,
            value = 0.0
        )
    )
    val discount: StateFlow<DiscountModel> = _discount.asStateFlow()
    
    private val _isValid = MutableStateFlow(true)
    val isValid: StateFlow<Boolean> = _isValid.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun setDiscountType(type: DiscountType) {
        _discount.update { it.copy(type = type) }
        validateDiscount()
    }
    
    fun setDiscountValue(value: Double) {
        _discount.update { it.copy(value = value) }
        validateDiscount()
    }
    
    fun getDiscountModel(): DiscountModel {
        return _discount.value
    }
    
    fun calculateDiscountAmount(from: Double): Double {
        val discount = _discount.value
        return when (discount.type) {
            DiscountType.PERCENTAGE -> {
                (from * discount.value / 100.0).coerceAtMost(from)
            }
            DiscountType.FIXED_AMOUNT -> {
                discount.value.coerceAtMost(from)
            }
        }
    }
    
    fun getQuickDiscountOptions(): List<DiscountModel> {
        return listOf(
            DiscountModel(DiscountType.PERCENTAGE, 5.0),
            DiscountModel(DiscountType.PERCENTAGE, 10.0),
            DiscountModel(DiscountType.PERCENTAGE, 15.0),
            DiscountModel(DiscountType.PERCENTAGE, 20.0),
            DiscountModel(DiscountType.FIXED_AMOUNT, 20.0),
            DiscountModel(DiscountType.FIXED_AMOUNT, 50.0),
            DiscountModel(DiscountType.FIXED_AMOUNT, 100.0)
        )
    }
    
    private fun validateDiscount() {
        val discount = _discount.value
        val isValid = discount.value >= 0 && (
            when (discount.type) {
                DiscountType.PERCENTAGE -> discount.value <= 100
                DiscountType.FIXED_AMOUNT -> true
            }
        )
        
        _isValid.value = isValid
        
        _errorMessage.value = when {
            discount.value < 0 -> "ค่าส่วนลดต้องไม่เป็นค่าลบ"
            discount.type == DiscountType.PERCENTAGE && discount.value > 100 -> "เปอร์เซ็นต์ส่วนลดต้องไม่เกิน 100%"
            else -> null
        }
    }
}

