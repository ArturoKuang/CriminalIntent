package com.example.criminalintent

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.lang.Exception
import java.text.DateFormat


private const val TAG = "CrimeListFragment"
private const val TYPE_CRIME = 0
private const val TYPE_SPECIAL_CRIME = 1

class CrimeListFragment : Fragment() {

    private lateinit var crimeRecyclerView: RecyclerView
    private var adapter: CrimeAdapter? = null

    private val crimeListViewModel: CrimeListViewModel by lazy {
        ViewModelProvider(this).get(CrimeListViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Total crimes: #${crimeListViewModel.crimes.size}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime_list, container, false)

        crimeRecyclerView =
            view.findViewById(R.id.crime_recycler_view) as RecyclerView
        crimeRecyclerView.layoutManager = LinearLayoutManager(context)

        updateUI()

        return view
    }

    private inner class CrimeHolder(view: View):
        RecyclerView.ViewHolder(view), View.OnClickListener {

            private lateinit var crime: Crime
            private val titleTextView: TextView = itemView.findViewById(R.id.crime_title)
            private val datTextView: TextView  = itemView.findViewById(R.id.crime_date)
            private val solvedImageView: ImageView = itemView.findViewById(R.id.crime_solved)

            init {
                itemView.setOnClickListener(this)
            }

            fun bind(crime: Crime) {
                this.crime = crime
                titleTextView.text = this.crime.title
                val df = DateFormat.getDateInstance(DateFormat.SHORT)
                datTextView.text = df.format(this.crime.date).toString()

                solvedImageView.visibility = if (crime.isSolved) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }

            override fun onClick(p0: View?) {
                Toast.makeText(context, "${crime.title} pressed", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private inner class SpecialCrimeHolder(view: View):
        RecyclerView.ViewHolder(view), View.OnClickListener {

        private lateinit var crime: Crime
        private val titleTextView: TextView = itemView.findViewById(R.id.crime_title)
        private val datTextView: TextView  = itemView.findViewById(R.id.crime_date)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(crime: Crime) {
            this.crime = crime
            titleTextView.text = this.crime.title
            val df = DateFormat.getDateInstance(DateFormat.SHORT)
            datTextView.text = df.format(this.crime.date).toString()
        }

        override fun onClick(p0: View?) {
            Toast.makeText(context, "${crime.title} pressed", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private inner class CrimeAdapter(var crimes: List<Crime>)
        : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
                : RecyclerView.ViewHolder {

            var view:View
            return when(viewType) {
                TYPE_SPECIAL_CRIME -> {
                    view = layoutInflater.inflate(R.layout.list_item_special_crime, parent, false)
                    SpecialCrimeHolder(view)
                }
                TYPE_CRIME -> {
                    view = layoutInflater.inflate(R.layout.list_item_crime, parent, false)
                    CrimeHolder(view)
                }
                else -> throw Exception("ViewType is not supported")
            }
        }

        override fun getItemCount(): Int = crimes.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val crime = crimes[position]

            if (holder is CrimeHolder) {
                holder.apply {
                    bind(crime)
                }
            } else if (holder is SpecialCrimeHolder) {
                holder.apply {
                    bind(crime)
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if(crimes[position].requiresPolice) {
                TYPE_SPECIAL_CRIME
            } else {
                TYPE_CRIME
            }
        }
    }

    private fun updateUI() {
        val crimes = crimeListViewModel.crimes
        adapter = CrimeAdapter(crimes)
        crimeRecyclerView.adapter = adapter
    }

    companion object {
        fun newInstance() : CrimeListFragment {
            return CrimeListFragment()
        }
    }
}