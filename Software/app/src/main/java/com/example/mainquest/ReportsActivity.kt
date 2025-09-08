package com.example.mainquest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReportsActivity : AppCompatActivity() {
    
    private lateinit var backButton: Button
    private lateinit var periodSpinner: Spinner
    private lateinit var generateReportButton: Button
    private lateinit var exportReportButton: Button
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var reportScrollView: ScrollView
    private lateinit var reportContainer: LinearLayout
    
    private lateinit var reportGenerator: ReportGenerator
    private var userId: Int = -1
    private var currentReport: ReportGenerator.DetailedReport? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)
        
        initViews()
        setupSpinner()
        setupClickListeners()
        
        reportGenerator = ReportGenerator(this)
        
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        userId = sharedPref.getInt("userId", -1)
        
        // Automatski generiraj početni izvještaj
        generateReport("Sveukupno")
    }
    
    private fun initViews() {
        backButton = findViewById(R.id.back_button)
        periodSpinner = findViewById(R.id.period_spinner)
        generateReportButton = findViewById(R.id.generate_report_button)
        exportReportButton = findViewById(R.id.export_report_button)
        loadingProgressBar = findViewById(R.id.loading_progress_bar)
        reportScrollView = findViewById(R.id.report_scroll_view)
        reportContainer = findViewById(R.id.report_container)
    }
    
    private fun setupSpinner() {
        val periods = listOf(
            "Sveukupno",
            "Danas",
            "Ovaj tjedan", 
            "Ovaj mjesec",
            "Zadnjih 7 dana",
            "Zadnjih 30 dana"
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        periodSpinner.adapter = adapter
    }
    
    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }
        
        generateReportButton.setOnClickListener {
            val selectedPeriod = periodSpinner.selectedItem as String
            generateReport(selectedPeriod)
        }
        
        exportReportButton.setOnClickListener {
            currentReport?.let { report ->
                showExportOptions(report)
            } ?: run {
                Toast.makeText(this, "Prvo generirajte izvještaj", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun generateReport(period: String) {
        if (userId == -1) {
            Toast.makeText(this, "Greška: Korisnik nije pronađen", Toast.LENGTH_SHORT).show()
            return
        }
        
        showLoading(true)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val report = reportGenerator.generateDetailedReport(userId, period)
                
                withContext(Dispatchers.Main) {
                    currentReport = report
                    displayReport(report)
                    showLoading(false)
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@ReportsActivity, "Greška pri generiranju izvještaja: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun displayReport(report: ReportGenerator.DetailedReport) {
        reportContainer.removeAllViews()
        
        // Header
        addReportHeader(report)
        
        // Korisničke informacije
        addUserInfoSection(report.userInfo)
        
        // Sažetak zadataka
        addTaskSummarySection(report.taskSummary)
        
        // Analiza kategorija
        addCategoryAnalysisSection(report.categoryAnalysis)
        
        // Analiza napretka
        addProgressAnalysisSection(report.progressAnalysis)
        
        // Analiza streakova
        addStreakAnalysisSection(report.streakAnalysis)
        
        // Preporuke
        addRecommendationsSection(report.recommendations)
        
        // Detaljne aktivnosti
        addDetailedActivitiesSection(report.detailedActivities)
        
        reportScrollView.visibility = View.VISIBLE
        exportReportButton.visibility = View.VISIBLE
    }
    
    private fun addReportHeader(report: ReportGenerator.DetailedReport) {
        val headerView = layoutInflater.inflate(R.layout.report_header, reportContainer, false)
        
        headerView.findViewById<TextView>(R.id.report_title).text = "📊 Detaljni izvještaj"
        headerView.findViewById<TextView>(R.id.report_period).text = "Period: ${report.period}"
        headerView.findViewById<TextView>(R.id.report_date).text = "Generirano: ${report.reportDate}"
        
        reportContainer.addView(headerView)
    }
    
    private fun addUserInfoSection(userInfo: ReportGenerator.UserReportInfo) {
        val sectionView = layoutInflater.inflate(R.layout.report_user_info, reportContainer, false)
        
        sectionView.findViewById<TextView>(R.id.username_text).text = userInfo.username
        sectionView.findViewById<TextView>(R.id.level_text).text = "Level ${userInfo.currentLevel}"
        sectionView.findViewById<TextView>(R.id.total_xp_text).text = "${userInfo.totalXp} XP"
        sectionView.findViewById<TextView>(R.id.xp_to_next_text).text = "Do sljedećeg levela: ${userInfo.xpToNextLevel} XP"
        
        reportContainer.addView(sectionView)
    }
    
    private fun addTaskSummarySection(summary: ReportGenerator.TaskSummary) {
        val sectionView = layoutInflater.inflate(R.layout.report_task_summary, reportContainer, false)
        
        sectionView.findViewById<TextView>(R.id.total_tasks_text).text = summary.totalTasks.toString()
        sectionView.findViewById<TextView>(R.id.completed_tasks_text).text = summary.completedTasks.toString()
        sectionView.findViewById<TextView>(R.id.completion_rate_text).text = "${String.format("%.1f", summary.completionRate)}%"
        sectionView.findViewById<TextView>(R.id.total_daily_goals_text).text = summary.totalDailyGoals.toString()
        sectionView.findViewById<TextView>(R.id.completed_daily_goals_text).text = summary.completedDailyGoals.toString()
        sectionView.findViewById<TextView>(R.id.daily_goal_rate_text).text = "${String.format("%.1f", summary.dailyGoalRate)}%"
        sectionView.findViewById<TextView>(R.id.total_xp_earned_text).text = "${summary.totalXpEarned} XP"
        sectionView.findViewById<TextView>(R.id.average_xp_text).text = "${String.format("%.1f", summary.averageXpPerTask)} XP"
        
        reportContainer.addView(sectionView)
    }
    
    private fun addCategoryAnalysisSection(categories: List<ReportGenerator.CategoryStats>) {
        val sectionView = layoutInflater.inflate(R.layout.report_category_analysis, reportContainer, false)
        val categoryContainer = sectionView.findViewById<LinearLayout>(R.id.category_container)
        
        categories.forEach { category ->
            val categoryView = layoutInflater.inflate(R.layout.report_category_item, categoryContainer, false)
            
            categoryView.findViewById<TextView>(R.id.category_name_text).text = category.categoryName
            categoryView.findViewById<TextView>(R.id.category_total_text).text = category.totalTasks.toString()
            categoryView.findViewById<TextView>(R.id.category_completed_text).text = category.completedTasks.toString()
            categoryView.findViewById<TextView>(R.id.category_rate_text).text = "${String.format("%.1f", category.completionRate)}%"
            categoryView.findViewById<TextView>(R.id.category_xp_text).text = "${category.totalXp} XP"
            
            categoryContainer.addView(categoryView)
        }
        
        reportContainer.addView(sectionView)
    }
    
    private fun addProgressAnalysisSection(progress: ReportGenerator.ProgressAnalysis) {
        val sectionView = layoutInflater.inflate(R.layout.report_progress_analysis, reportContainer, false)
        
        sectionView.findViewById<TextView>(R.id.today_completed_text).text = progress.todayCompleted.toString()
        sectionView.findViewById<TextView>(R.id.weekly_completed_text).text = progress.weeklyCompleted.toString()
        sectionView.findViewById<TextView>(R.id.monthly_completed_text).text = progress.monthlyCompleted.toString()
        sectionView.findViewById<TextView>(R.id.weekly_average_text).text = String.format("%.1f", progress.weeklyAverage)
        sectionView.findViewById<TextView>(R.id.monthly_average_text).text = String.format("%.1f", progress.monthlyAverage)
        sectionView.findViewById<TextView>(R.id.best_day_text).text = "${progress.bestDay} (${progress.bestDayCount})"
        
        reportContainer.addView(sectionView)
    }
    
    private fun addStreakAnalysisSection(streak: ReportGenerator.StreakAnalysis) {
        val sectionView = layoutInflater.inflate(R.layout.report_streak_analysis, reportContainer, false)
        
        sectionView.findViewById<TextView>(R.id.current_streak_text).text = streak.currentStreak.toString()
        sectionView.findViewById<TextView>(R.id.longest_streak_text).text = streak.longestStreak.toString()
        sectionView.findViewById<TextView>(R.id.streak_trend_text).text = streak.streakTrend
        
        val streakContainer = sectionView.findViewById<LinearLayout>(R.id.streak_categories_container)
        streak.streakCategories.forEach { (category, streakCount) ->
            val streakView = TextView(this)
            streakView.text = "$category: $streakCount dana"
            streakView.setPadding(0, 8, 0, 8)
            streakContainer.addView(streakView)
        }
        
        reportContainer.addView(sectionView)
    }
    
    private fun addRecommendationsSection(recommendations: List<String>) {
        val sectionView = layoutInflater.inflate(R.layout.report_recommendations, reportContainer, false)
        val recommendationsContainer = sectionView.findViewById<LinearLayout>(R.id.recommendations_container)
        
        recommendations.forEach { recommendation ->
            val recommendationView = TextView(this)
            recommendationView.text = "• $recommendation"
            recommendationView.setPadding(0, 8, 0, 8)
            recommendationView.textSize = 14f
            recommendationsContainer.addView(recommendationView)
        }
        
        reportContainer.addView(sectionView)
    }
    
    private fun addDetailedActivitiesSection(activities: List<ReportGenerator.ActivityReport>) {
        val sectionView = layoutInflater.inflate(R.layout.report_detailed_activities, reportContainer, false)
        val activitiesContainer = sectionView.findViewById<LinearLayout>(R.id.activities_container)
        
        activities.take(20).forEach { activity -> // Prikaži samo prvih 20
            val activityView = layoutInflater.inflate(R.layout.report_activity_item, activitiesContainer, false)
            
            activityView.findViewById<TextView>(R.id.activity_date_text).text = activity.date
            activityView.findViewById<TextView>(R.id.activity_time_text).text = activity.time
            activityView.findViewById<TextView>(R.id.activity_task_text).text = activity.taskName
            activityView.findViewById<TextView>(R.id.activity_category_text).text = activity.category
            activityView.findViewById<TextView>(R.id.activity_xp_text).text = "+${activity.xpEarned} XP"
            activityView.findViewById<TextView>(R.id.activity_type_text).text = activity.taskType
            
            activitiesContainer.addView(activityView)
        }
        
        if (activities.size > 20) {
            val moreView = TextView(this)
            moreView.text = "... i još ${activities.size - 20} aktivnosti"
            moreView.setPadding(0, 16, 0, 8)
            moreView.textSize = 12f
            activitiesContainer.addView(moreView)
        }
        
        reportContainer.addView(sectionView)
    }
    
    private fun showExportOptions(report: ReportGenerator.DetailedReport) {
        val options = arrayOf(
            "📄 Detaljni izvještaj (TXT)",
            "📊 Aktivnosti (CSV)", 
            "📋 Kratki sažetak (TXT)"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Odaberite format izvoza")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportDetailedReport(report)
                    1 -> exportCSVReport(report)
                    2 -> exportSummaryReport(report)
                }
            }
            .show()
    }
    
    private fun exportDetailedReport(report: ReportGenerator.DetailedReport) {
        // Pozovi ReportExporter
        val exporter = ReportExporter(this)
        exporter.exportReportAsText(report) { success, filePath ->
            if (success && filePath != null) {
                val fileName = filePath.substringAfterLast("/")
                val location = if (filePath.contains("Download")) {
                    "Downloads folder"
                } else {
                    "aplikacijski folder"
                }
                
                Toast.makeText(this, "✅ Izvještaj spremljen!\n📁 $fileName\n📍 Lokacija: $location", Toast.LENGTH_LONG).show()
                
                // Ponudi dijeljenje
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Main Quest - Izvještaj aktivnosti")
                    putExtra(Intent.EXTRA_TEXT, "Izvještaj o aktivnostima iz Main Quest aplikacije.")
                }
                startActivity(Intent.createChooser(shareIntent, "Podijeli izvještaj"))
                
            } else {
                Toast.makeText(this, "❌ Greška pri izvozu izvještaja", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun exportCSVReport(report: ReportGenerator.DetailedReport) {
        val exporter = ReportExporter(this)
        exporter.exportReportAsCSV(report) { success, filePath ->
            if (success && filePath != null) {
                val fileName = filePath.substringAfterLast("/")
                val location = if (filePath.contains("Download")) {
                    "Downloads folder"
                } else {
                    "aplikacijski folder"
                }
                
                Toast.makeText(this, "✅ CSV datoteka spremljena!\n📁 $fileName\n📍 Lokacija: $location", Toast.LENGTH_LONG).show()
                
            } else {
                Toast.makeText(this, "❌ Greška pri izvozu CSV datoteke", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun exportSummaryReport(report: ReportGenerator.DetailedReport) {
        val exporter = ReportExporter(this)
        exporter.exportSummaryReport(report) { success, filePath ->
            if (success && filePath != null) {
                val fileName = filePath.substringAfterLast("/")
                val location = if (filePath.contains("Download")) {
                    "Downloads folder"
                } else {
                    "aplikacijski folder"
                }
                
                Toast.makeText(this, "✅ Sažetak spremljen!\n📁 $fileName\n📍 Lokacija: $location", Toast.LENGTH_LONG).show()
                
                // Ponudi dijeljenje
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Main Quest - Kratki sažetak")
                    putExtra(Intent.EXTRA_TEXT, "Kratki sažetak aktivnosti iz Main Quest aplikacije.")
                }
                startActivity(Intent.createChooser(shareIntent, "Podijeli sažetak"))
                
            } else {
                Toast.makeText(this, "❌ Greška pri izvozu sažetka", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        loadingProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        generateReportButton.isEnabled = !show
        exportReportButton.visibility = if (show) View.GONE else View.VISIBLE
        reportScrollView.visibility = if (show) View.GONE else View.VISIBLE
    }
} 