package com.example.mycalculator

// import statements: libraries and classes needed for this activity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import net.objecthunter.exp4j.ExpressionBuilder

class MainActivity : AppCompatActivity() {

    // TextViews to show user input (matchOperation) and result (matchResult)
    private lateinit var matchOperation: TextView
    private lateinit var matchResult: TextView

    private var lastNumeric = false   // flag: was the last input a digit?
    private var lastDot = false       // flag: does the current number already have a decimal point?
    private var lastOperator = false  // flag: was the last input an operator?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // enable full screen layout on modern Android devices
        setContentView(R.layout.activity_main)

        // initialize our TextView fields from the layout
        matchOperation = findViewById(R.id.match_operation)
        matchResult = findViewById(R.id.match_result)

        // apply extra bottom padding to avoid overlapping with navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraBottomPadding = resources.getDimensionPixelSize(R.dimen.extra_bottom_padding)
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom + extraBottomPadding
            )
            insets
        }
    }

    fun onButtonClick(view: View) {
        val button = view as MaterialButton
        when (view.id) {
            R.id.ac_btn -> clearAll()          // clear all input
            R.id.btn_back -> deleteLastChar()  // delete the last character
            R.id.btn_ravno -> calculateResult() // calculate the expression
            else -> {
                val text = button.text.toString()
                // Check if the clicked button is an operator, a dot, or a digit
                when {
                    text.matches("[÷×/\\+\\-()%]".toRegex()) -> handleOperator(text)
                    text == "." -> handleDecimalPoint()
                    text.matches("\\d".toRegex()) -> handleNumber(text)
                }
            }
        }
    }

    private fun handleNumber(number: String) {
        val currentText = matchOperation.text.toString()
        if (currentText == "0") {
            matchOperation.text = number
        } else {
            matchOperation.append(number)
        }
        lastNumeric = true
        lastOperator = false
        lastDot = false
    }

    private fun handleOperator(operator: String) {
        if (lastNumeric || operator == "(") {
            val currentText = matchOperation.text.toString()
            val newOperator = when (operator) {
                "÷" -> "/"
                "×" -> "*"
                else -> operator
            }
            // if the last character was also an operator (and this one is not a bracket),
            // replace the previous operator instead of appending a new one
            if (lastOperator && operator !in listOf("(", ")")) {
                matchOperation.text = currentText.dropLast(1) + newOperator
            } else {
                matchOperation.append(newOperator)
            }
            lastNumeric = operator == ")"
            lastOperator = operator !in listOf("(", ")")
            lastDot = false
        }
    }

    private fun handleDecimalPoint() {
        if (lastNumeric && !lastDot) {
            matchOperation.append(".")
            lastDot = true
            lastNumeric = false
        }
    }

    private fun deleteLastChar() {
        val text = matchOperation.text.toString()
        if (text.isNotEmpty()) {
            matchOperation.text = text.dropLast(1)
            when {
                text.last() == '.' -> lastDot = false
                text.last().isDigit() -> lastNumeric = true
                else -> {
                    lastOperator = true
                    lastNumeric = false
                }
            }
        }
    }

    private fun clearAll() {
        matchOperation.text = ""
        matchResult.text = ""
        lastNumeric = false
        lastDot = false
        lastOperator = false
    }

    private fun calculateResult() {
        try {
            val expression = ExpressionBuilder(matchOperation.text.toString()).build()
            val result = expression.evaluate()

            val longResult = result.toLong()
            matchResult.text =
                if (result == longResult.toDouble()) longResult.toString() else result.toString()
        } catch (e: Exception) {
            showError("Invalid expression")
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        matchResult.text = "Error"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("operation", matchOperation.text.toString())
        outState.putString("result", matchResult.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        matchOperation.text = savedInstanceState.getString("operation", "")
        matchResult.text = savedInstanceState.getString("result", "")
    }
}
