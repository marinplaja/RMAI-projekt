package com.example.mainquest

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

// Extension function for older Kotlin versions
fun StringBuilder.appendLine(text: String = "") {
    append(text).append("\n")
}

class ReportExporter(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    
    fun exportReportAsText(report: ReportGenerator.DetailedReport, callback: (Boolean, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fileName = "MainQuest_Izvještaj_${dateFormat.format(Date())}.txt"
                val file = createFile(fileName)
                
                if (file != null) {
                    val content = generateTextReport(report)
                    FileWriter(file).use { writer ->
                        writer.write(content)
                    }
                    
                    withContext(Dispatchers.Main) {
                        callback(true, file.absolutePath)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback(false, null)
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(false, null)
                }
            }
        }
    }
    
    fun exportReportAsCSV(report: ReportGenerator.DetailedReport, callback: (Boolean, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fileName = "MainQuest_Aktivnosti_${dateFormat.format(Date())}.csv"
                val file = createFile(fileName)
                
                if (file != null) {
                    val content = generateCSVReport(report)
                    FileWriter(file).use { writer ->
                        writer.write(content)
                    }
                    
                    withContext(Dispatchers.Main) {
                        callback(true, file.absolutePath)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback(false, null)
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(false, null)
                }
            }
        }
    }
    
    private fun createFile(fileName: String): File? {
        return try {
            // Pokušaj kreirati u Downloads direktoriju
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = File(downloadsDir, fileName)
            if (file.exists()) {
                file.delete()
            }
            file.createNewFile()
            file
            
        } catch (e: Exception) {
            try {
                // Fallback - koristi interni storage
                val file = File(context.filesDir, fileName)
                if (file.exists()) {
                    file.delete()
                }
                file.createNewFile()
                file
            } catch (e2: Exception) {
                null
            }
        }
    }
    
    private fun generateTextReport(report: ReportGenerator.DetailedReport): String {
        val sb = StringBuilder()
        
        // Header
        sb.appendLine("═══════════════════════════════════════════════════════")
        sb.appendLine("                 📊 MAIN QUEST IZVJEŠTAJ")
        sb.appendLine("═══════════════════════════════════════════════════════")
        sb.appendLine()
        sb.appendLine("📅 Datum generiranja: ${report.reportDate}")
        sb.appendLine("⏰ Period: ${report.period}")
        sb.appendLine()
        
        // Korisničke informacije
        sb.appendLine("👤 KORISNIČKE INFORMACIJE")
        sb.appendLine("─────────────────────────────────────────────────────")
        sb.appendLine("Korisničko ime: ${report.userInfo.username}")
        sb.appendLine("Trenutni level: ${report.userInfo.currentLevel}")
        sb.appendLine("Ukupno XP: ${report.userInfo.totalXp}")
        sb.appendLine("XP do sljedećeg levela: ${report.userInfo.xpToNextLevel}")
        sb.appendLine("Datum pridruživanja: ${report.userInfo.joinDate}")
        sb.appendLine()
        
        // Sažetak zadataka
        sb.appendLine("📋 SAŽETAK ZADATAKA")
        sb.appendLine("─────────────────────────────────────────────────────")
        sb.appendLine("Ukupno zadataka: ${report.taskSummary.totalTasks}")
        sb.appendLine("Završenih zadataka: ${report.taskSummary.completedTasks}")
        sb.appendLine("Stopa završetka: ${String.format("%.1f", report.taskSummary.completionRate)}%")
        sb.appendLine("Ukupno dnevnih ciljeva: ${report.taskSummary.totalDailyGoals}")
        sb.appendLine("Završenih dnevnih ciljeva: ${report.taskSummary.completedDailyGoals}")
        sb.appendLine("Stopa dnevnih ciljeva: ${String.format("%.1f", report.taskSummary.dailyGoalRate)}%")
        sb.appendLine("Ukupno XP zarada: ${report.taskSummary.totalXpEarned}")
        sb.appendLine("Prosjek XP po zadatku: ${String.format("%.1f", report.taskSummary.averageXpPerTask)}")
        sb.appendLine()
        
        // Analiza kategorija
        sb.appendLine("📊 ANALIZA PO KATEGORIJAMA")
        sb.appendLine("─────────────────────────────────────────────────────")
        report.categoryAnalysis.forEach { category ->
            sb.appendLine("${category.categoryName}:")
            sb.appendLine("  • Ukupno: ${category.totalTasks} | Završeno: ${category.completedTasks}")
            sb.appendLine("  • Stopa: ${String.format("%.1f", category.completionRate)}% | XP: ${category.totalXp}")
            sb.appendLine("  • Prosjek XP: ${String.format("%.1f", category.averageXp)}")
            sb.appendLine()
        }
        
        // Analiza napretka
        sb.appendLine("📈 ANALIZA NAPRETKA")
        sb.appendLine("─────────────────────────────────────────────────────")
        sb.appendLine("Danas završeno: ${report.progressAnalysis.todayCompleted}")
        sb.appendLine("Ovaj tjedan završeno: ${report.progressAnalysis.weeklyCompleted}")
        sb.appendLine("Ovaj mjesec završeno: ${report.progressAnalysis.monthlyCompleted}")
        sb.appendLine("Tjedni prosjek: ${String.format("%.1f", report.progressAnalysis.weeklyAverage)}")
        sb.appendLine("Mjesečni prosjek: ${String.format("%.1f", report.progressAnalysis.monthlyAverage)}")
        sb.appendLine("Najbolji dan: ${report.progressAnalysis.bestDay} (${report.progressAnalysis.bestDayCount} zadataka)")
        sb.appendLine()
        
        // Analiza streakova
        sb.appendLine("🔥 ANALIZA STREAKOVA")
        sb.appendLine("─────────────────────────────────────────────────────")
        sb.appendLine("Trenutni streak: ${report.streakAnalysis.currentStreak} dana")
        sb.appendLine("Najduži streak: ${report.streakAnalysis.longestStreak} dana")
        sb.appendLine("Trend: ${report.streakAnalysis.streakTrend}")
        sb.appendLine("Streakovi po kategorijama:")
        report.streakAnalysis.streakCategories.forEach { (category, streak) ->
            sb.appendLine("  • $category: $streak dana")
        }
        sb.appendLine()
        
        // Preporuke
        sb.appendLine("💡 PREPORUKE")
        sb.appendLine("─────────────────────────────────────────────────────")
        report.recommendations.forEach { recommendation ->
            sb.appendLine("• $recommendation")
        }
        sb.appendLine()
        
        // Detaljne aktivnosti
        sb.appendLine("📝 DETALJNE AKTIVNOSTI (zadnjih ${report.detailedActivities.size})")
        sb.appendLine("─────────────────────────────────────────────────────")
        report.detailedActivities.forEach { activity ->
            sb.appendLine("${activity.date} ${activity.time} | ${activity.taskName}")
            sb.appendLine("  Kategorija: ${activity.category} | Tip: ${activity.taskType}")
            sb.appendLine("  XP: +${activity.xpEarned}")
            sb.appendLine()
        }
        
        sb.appendLine("═══════════════════════════════════════════════════════")
        sb.appendLine("Generirano iz Main Quest aplikacije")
        sb.appendLine("═══════════════════════════════════════════════════════")
        
        return sb.toString()
    }
    
    private fun generateCSVReport(report: ReportGenerator.DetailedReport): String {
        val sb = StringBuilder()
        
        // CSV Header
        sb.appendLine("Datum,Vrijeme,Naziv_zadatka,Kategorija,XP,Tip_zadatka")
        
        // CSV Data
        report.detailedActivities.forEach { activity ->
            sb.appendLine("\"${activity.date}\",\"${activity.time}\",\"${activity.taskName}\",\"${activity.category}\",${activity.xpEarned},\"${activity.taskType}\"")
        }
        
        return sb.toString()
    }
    
    fun exportSummaryReport(report: ReportGenerator.DetailedReport, callback: (Boolean, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fileName = "MainQuest_Sažetak_${dateFormat.format(Date())}.txt"
                val file = createFile(fileName)
                
                if (file != null) {
                    val content = generateSummaryReport(report)
                    FileWriter(file).use { writer ->
                        writer.write(content)
                    }
                    
                    withContext(Dispatchers.Main) {
                        callback(true, file.absolutePath)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback(false, null)
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(false, null)
                }
            }
        }
    }
    
    private fun generateSummaryReport(report: ReportGenerator.DetailedReport): String {
        val sb = StringBuilder()
        
        sb.appendLine("📊 MAIN QUEST - KRATKI SAŽETAK")
        sb.appendLine("═══════════════════════════════")
        sb.appendLine("Korisnik: ${report.userInfo.username}")
        sb.appendLine("Period: ${report.period}")
        sb.appendLine("Datum: ${report.reportDate}")
        sb.appendLine()
        
        sb.appendLine("🎯 KLJUČNE STATISTIKE:")
        sb.appendLine("• Level: ${report.userInfo.currentLevel} (${report.userInfo.totalXp} XP)")
        sb.appendLine("• Završeno zadataka: ${report.taskSummary.completedTasks}/${report.taskSummary.totalTasks}")
        sb.appendLine("• Stopa uspjeha: ${String.format("%.1f", report.taskSummary.completionRate)}%")
        sb.appendLine("• Ukupno XP: ${report.taskSummary.totalXpEarned}")
        sb.appendLine("• Trenutni streak: ${report.streakAnalysis.currentStreak} dana")
        sb.appendLine()
        
        sb.appendLine("📈 NAPREDAK:")
        sb.appendLine("• Danas: ${report.progressAnalysis.todayCompleted}")
        sb.appendLine("• Ovaj tjedan: ${report.progressAnalysis.weeklyCompleted}")
        sb.appendLine("• Ovaj mjesec: ${report.progressAnalysis.monthlyCompleted}")
        sb.appendLine()
        
        sb.appendLine("🏆 TOP KATEGORIJA:")
        val topCategory = report.categoryAnalysis.firstOrNull()
        if (topCategory != null) {
            sb.appendLine("• ${topCategory.categoryName}: ${topCategory.completedTasks} zadataka")
        }
        sb.appendLine()
        
        sb.appendLine("💡 GLAVNA PREPORUKA:")
        sb.appendLine("• ${report.recommendations.firstOrNull() ?: "Nastavi odličan rad!"}")
        
        return sb.toString()
    }
} 