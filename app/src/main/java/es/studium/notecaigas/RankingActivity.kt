package es.studium.notecaigas

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RankingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)

        val lvRanking = findViewById<ListView>(R.id.lvRanking)
        val btnBack = findViewById<Button>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        loadRankingData(lvRanking)
    }

    private fun loadRankingData(lvRanking: ListView) {
        val dbHelper = RankingDbHelper(this)
        val scores = dbHelper.getTopScores(20)

        val customAdapter = object : BaseAdapter() {
            override fun getCount(): Int = scores.size
            override fun getItem(position: Int): Any = scores[position]
            override fun getItemId(position: Int): Long = scores[position].id.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = convertView ?: LayoutInflater.from(this@RankingActivity)
                    .inflate(android.R.layout.simple_list_item_2, parent, false)
                val item = scores[position]
                val text1 = view.findViewById<TextView>(android.R.id.text1)
                val text2 = view.findViewById<TextView>(android.R.id.text2)

                text1.text = getString(R.string.ranking_item_format, position + 1, item.name, item.timeSeconds)
                text1.setTextColor(Color.WHITE)
                text1.textSize = 18f

                text2.text = getString(R.string.date_format, item.date)
                text2.setTextColor(Color.LTGRAY)
                text2.textSize = 14f

                return view
            }
        }

        lvRanking.adapter = customAdapter
    }
}
