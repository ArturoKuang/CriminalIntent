package com.example.criminalintent

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.lang.Exception
import java.text.DateFormat
import java.util.*


private const val TAG = "CrimeListFragment"
private const val TYPE_CRIME = 0
private const val TYPE_SPECIAL_CRIME = 1

class CrimeListFragment : Fragment() {

    interface Callbacks {
        fun onCrimeSelected(crimeId: UUID)
    }

    private var callbacks: Callbacks? = null
    private lateinit var crimeRecyclerView: RecyclerView
    private var adapter: CrimeAdapter? = CrimeAdapter()

    private val crimeListViewModel: CrimeListViewModel by lazy {
        ViewModelProvider(this).get(CrimeListViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
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
        crimeRecyclerView.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onviewcreated")
        crimeListViewModel.crimesListLiveData.observe(
            viewLifecycleOwner,
            Observer {
                crimes ->
                    crimes?.let {
                        Log.i(TAG, "Got crimes ${crimes.size}")
                        updateUI(crimes as MutableList<Crime>)
                    }
            }
        )
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
                val df = DateFormat.getDateInstance(DateFormat.LONG)
                datTextView.text = df.format(this.crime.date).toString()

                solvedImageView.visibility = if (crime.isSolved) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }

            override fun onClick(p0: View?) {
                callbacks?.onCrimeSelected(crime.id)
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
            datTextView.text = this.crime.date.toString()
        }

        override fun onClick(p0: View?) {
            callbacks?.onCrimeSelected(crime.id)
        }
    }

    private inner class CrimeAdapter()
        : ListAdapter<Crime, RecyclerView.ViewHolder>(CrimeDiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
                : RecyclerView.ViewHolder {
            Log.i(TAG, "Adapter onCreateViewHolder")
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

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            Log.i(TAG, "Adapter onBindViewHolder")

            if (holder is CrimeHolder) {
                holder.apply {
                    bind(getItem(position))
                }
            } else if (holder is SpecialCrimeHolder) {
                holder.apply {
                    bind(getItem(position))
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            //temp. fix
            return if(getItem(position).title == "null" ) {
                TYPE_SPECIAL_CRIME
            } else {
                TYPE_CRIME
            }
        }
    }

    private fun updateUI(crimes: MutableList<Crime>) {
        adapter?.let {
            Log.d(TAG, "submitList")
            it.submitList(crimes)
        } ?: run {
            adapter = CrimeAdapter()
        }
        crimeRecyclerView.adapter = adapter
    }

    private inner class CrimeDiffCallback : DiffUtil.ItemCallback<Crime>() {
        override fun areItemsTheSame(oldItem: Crime, newItem: Crime): Boolean {
            Log.d(TAG, "areItemsSame")
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: Crime, newItem: Crime): Boolean {
            Log.d(TAG, "areContentsSame")
            return oldItem == newItem
        }
    }

    companion object {
        fun newInstance() : CrimeListFragment {
            return CrimeListFragment()
        }
    }
}