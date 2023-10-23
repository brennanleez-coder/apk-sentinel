package com.example.apksentinel.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class DashboardFragment : Fragment() {

    private lateinit var apkItemDao: ApkItemDao

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = activity?.application as? ApkSentinel
        app?.isInitialized?.observe(viewLifecycleOwner) { initialized ->
            if (initialized) {
                initDashboard(view)
            }
        }
    }

    private fun initDashboard(view: View) {

        apkItemDao = ApkItemDatabase.getDatabase(this.requireContext()).apkItemDao()
        val pie = AnyChart.pie()
        val pieChartView: AnyChartView = view.findViewById(R.id.pie_chart)
        APIlib.getInstance().setActiveAnyChartView(pieChartView);

        pieChartView.setProgressBar(view.findViewById(R.id.progress_bar))


        coroutineScope.launch {
            try {
                val systemAppsCount = apkItemDao.countSystemApps()
                val nonSystemAppsCount = apkItemDao.countNonSystemApps()
                val appsByPermissionCount = apkItemDao.getAppsByPermissionCount()
//                Log.d("Apk Sentinel", systemAppsCount.toString())
//                Log.d("Apk Sentinel", nonSystemAppsCount.toString())
//                Log.d("Apk Sentinel", appsByPermissionCount.toString())

                withContext(Dispatchers.Main) {
                    val data: MutableList<DataEntry> = ArrayList()
                    data.add(ValueDataEntry("System Apps", systemAppsCount))
                    data.add(ValueDataEntry("Non-System Apps", nonSystemAppsCount))
                    pie.data(data)

                    pie.title("Distribution of Applications")
                    pie.labels().position("outside")
                    pie.legend()
                        .position("center")
                        .itemsLayout(LegendLayout.HORIZONTAL)
                        .align(Align.CENTER)

                    pieChartView.setChart(pie)

//                   Uncomment this chunk to show Bar Chart
//                    val barChartView: AnyChartView = view.findViewById(R.id.bar_chart)
//                    APIlib.getInstance().setActiveAnyChartView(barChartView);
//
//
//                    val cartesian: Cartesian = AnyChart.column()
//                    val barChartData: MutableList<DataEntry> = ArrayList()
//                    for (app in appsByPermissionCount) {
//                        barChartData.add(ValueDataEntry(app.appName, app.permissionsCount))
//                    }
//
//                    val column: Column = cartesian.column(barChartData)
//
//                    column.tooltip()
//                        .titleFormat("{%X}")
//                        .position(Position.CENTER_BOTTOM)
//                        .anchor(Anchor.CENTER_BOTTOM)
//                        .offsetX(0.0)
//                        .offsetY(5.0)
//                        .format("\${%Value}{groupsSeparator: }")
//
//                    cartesian.animation(true)
//                    cartesian.title("Most permissions used")
//
//                    cartesian.yScale().minimum(0.0)
//
//                    cartesian.yAxis(0).labels().format("\${%Value}{groupsSeparator: }")
//
//                    cartesian.tooltip().positionMode(TooltipPositionMode.POINT)
//                    cartesian.interactivity().hoverMode(HoverMode.BY_X)
//
//                    cartesian.xAxis(0).title("Product")
//                    cartesian.yAxis(0).title("Revenue")
//
//                    barChartView.setChart(cartesian)

                }

            } catch (exception: Exception)
            {
                Log.d("Apk Sentinel", "Failed to retrieve data ${exception.message}" )
            }

        }

        pie.setOnClickListener(object :
            ListenersInterface.OnClickListener(arrayOf<String>("x", "value")) {
            override fun onClick(event: Event) {
                Toast.makeText(
                    context,
                    event.data["x"] + ":" + event.data["value"],
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

    }
    companion object {
        fun newInstance(): DashboardFragment {
            return DashboardFragment()
        }
    }
}