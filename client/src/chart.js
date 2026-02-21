import * as echarts from "echarts"

export function initChart() {
  const chartDom = document.getElementById("chart")

  if (!chartDom) return

  const myChart = echarts.init(chartDom)

  const option = {
    title: {
      text: "ECharts Introduction Example",
    },
    tooltip: {},
    xAxis: {
      data: ["A", "B", "C", "D", "E", "F"],
    },
    yAxis: {},
    series: [
      {
        name: "Sales",
        type: "bar",
        data: [5, 20, 36, 10, 10, 20],
      },
    ],
  }

  myChart.setOption(option)
}