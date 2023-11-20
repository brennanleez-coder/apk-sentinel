package com.example.apksentinel.fragment

import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.anychart.APIlib
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.chart.common.listener.Event
import com.anychart.chart.common.listener.ListenersInterface
import com.anychart.enums.Align
import com.anychart.enums.LegendLayout
import com.example.apksentinel.ApkSentinel
import com.example.apksentinel.R
import com.example.apksentinel.database.ApkItemDatabase
import com.example.apksentinel.database.dao.ApkItemDao
import com.example.apksentinel.model.AppPermissionCount
import com.example.apksentinel.repository.AppRepository
import com.example.apksentinel.viewmodel.DashboardViewModel
import com.example.apksentinel.viewmodel.factory.DashboardViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class DashboardFragment : Fragment() {

    private lateinit var apkItemDao: ApkItemDao

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var viewModel: DashboardViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            val application = requireActivity().application as ApkSentinel
            val repository = AppRepository(ApkItemDatabase.getDatabase(requireContext()).apkItemDao())

            val viewModelFactory = DashboardViewModelFactory(application, repository)
            viewModel = ViewModelProvider(this, viewModelFactory).get(DashboardViewModel::class.java)
//            val isInitialised = viewModel.isInitialized

//            if (isInitialised.value == true) {
                observeViewModel(view)
//            }
        } catch (e: Exception) {
            Log.e("Apk Sentinel", "${e.printStackTrace().toString()}")
        }
    }

    private fun observeViewModel(view: View) {
        viewModel.systemAppsCount.observe(viewLifecycleOwner) { systemAppsCount ->
            viewModel.nonSystemAppsCount.observe(viewLifecycleOwner) { nonSystemAppsCount ->
                initPieChart(view, systemAppsCount, nonSystemAppsCount)
            }
        }
        // Add observers for any other LiveData from your ViewModel
    }
    private fun initPieChart(view: View, systemAppsCount: Int, nonSystemAppsCount: Int) {
        val pieChartView: AnyChartView = view.findViewById(R.id.pie_chart)
        APIlib.getInstance().setActiveAnyChartView(pieChartView)
        pieChartView.setProgressBar(view.findViewById(R.id.progress_bar))

        val pie = AnyChart.pie()
        val data: MutableList<DataEntry> = ArrayList()
        data.add(ValueDataEntry("System Apps", systemAppsCount))
        data.add(ValueDataEntry("Non-System Apps", nonSystemAppsCount))
        pie.data(data)

        pie.title("Distribution of Installed Applications")
        pie.labels().position("outside")
        pie.legend().position("center").itemsLayout(LegendLayout.HORIZONTAL).align(Align.CENTER)

        pieChartView.setChart(pie)

        pie.setOnClickListener(object :
            ListenersInterface.OnClickListener(arrayOf("x", "value")) {
            override fun onClick(event: Event) {
                if (isAdded) { // Check if the fragment is still added
                    Toast.makeText(
                        context,
                        event.data["x"] + ":" + event.data["value"],
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        val app = activity?.application as? ApkSentinel
//        app?.isInitialized?.removeObservers(viewLifecycleOwner)
    }


    companion object {
        fun newInstance(): DashboardFragment {
            return DashboardFragment()
        }
    }
}