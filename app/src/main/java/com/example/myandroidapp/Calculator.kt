package com.example.myandroidapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import net.objecthunter.exp4j.ExpressionBuilder

class Calculator : AppCompatActivity() {
    private lateinit var math_result: TextView; private lateinit var math_operation: TextView
    private lateinit var btnC: Button; private lateinit var bracket1: Button
    private lateinit var  bracket2: Button; private lateinit var del: Button
    private lateinit var  btn7: Button; private lateinit var btn8: Button
    private lateinit var btn9: Button; private lateinit var btnmultiply: Button
    private lateinit var btn4: Button; private lateinit var btn5: Button
    private lateinit var btn6: Button; private lateinit var btnminus: Button
    private lateinit var btn1: Button; private lateinit var btn2: Button
    private lateinit var btn3: Button; private lateinit var btnplus: Button
    private lateinit var btnAC: Button; private lateinit var btn0: Button
    private lateinit var btnDot: Button; private lateinit var btnRavno: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calculator)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        math_result = findViewById(R.id.math_result); math_operation = findViewById(R.id.math_operation)
        btnC = findViewById(R.id.btnC); bracket1 = findViewById(R.id.bracket1)
        bracket2 = findViewById(R.id.bracket2); del = findViewById(R.id.del)
        btn7 = findViewById(R.id.btn7); btn8 = findViewById(R.id.btn8)
        btn9 = findViewById(R.id.btn9); btnmultiply = findViewById(R.id.btnMultiply)
        btn4 = findViewById(R.id.btn4); btn5 = findViewById(R.id.btn5)
        btn6 = findViewById(R.id.btn6); btnminus = findViewById(R.id.btnMinus)
        btn1 = findViewById(R.id.btn1); btn2 = findViewById(R.id.btn2)
        btn3 = findViewById(R.id.btn3); btnplus = findViewById(R.id.btnPlus)
        btnAC = findViewById(R.id.btnAC); btn0 = findViewById(R.id.btn0)
        btnDot = findViewById(R.id.btnDot); btnRavno = findViewById(R.id.btnRavno)

    }

    override fun onStart() {
        super.onStart()
        btnC.setOnClickListener {
            val currentText = math_operation.text.toString()
            if (currentText.isNotEmpty()) {
                math_operation.text = currentText.substring(0, currentText.length - 1)
            }
        }
        bracket1.setOnClickListener { math_operation.text = math_operation.text.toString() + "(" }
        bracket2.setOnClickListener { math_operation.text = math_operation.text.toString() + ")" }
        del.setOnClickListener { math_operation.text = math_operation.text.toString() + "/" }
        btn7.setOnClickListener { math_operation.text = math_operation.text.toString() + "7" }
        btn8.setOnClickListener { math_operation.text = math_operation.text.toString() + "8" }
        btn9.setOnClickListener { math_operation.text = math_operation.text.toString() + "9" }
        btnmultiply.setOnClickListener { math_operation.text = math_operation.text.toString() + "*" }
        btn4.setOnClickListener { math_operation.text = math_operation.text.toString() + "4" }
        btn5.setOnClickListener { math_operation.text = math_operation.text.toString() + "5" }
        btn6.setOnClickListener { math_operation.text = math_operation.text.toString() + "6" }
        btnminus.setOnClickListener { math_operation.text = math_operation.text.toString() + "-" }
        btn1.setOnClickListener { math_operation.text = math_operation.text.toString() + "1" }
        btn2.setOnClickListener { math_operation.text = math_operation.text.toString() + "2" }
        btn3.setOnClickListener { math_operation.text = math_operation.text.toString() + "3" }
        btnplus.setOnClickListener { math_operation.text = math_operation.text.toString() + "+" }
        btnAC.setOnClickListener { math_operation.text = ""; math_result.text = "" }
        btn0.setOnClickListener { math_operation.text = math_operation.text.toString() + "0" }
        btnDot.setOnClickListener { math_operation.text = math_operation.text.toString() + "." }
        btnRavno.setOnClickListener {
            val expressionText = math_operation.text.toString()
            if (expressionText.isNotEmpty()) {
                try {
                    val expression = ExpressionBuilder(math_operation.text.toString()).build()
                    val result = expression.evaluate()
                    math_result.text = result.toString()
                } catch (e: Exception) {
                    math_result.text = "error"
                }
            }
        }
    }
}