package com.example.mainquest.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        User::class,
        Task::class,
        Reward::class,
        UnlockedReward::class,
        Challenge::class,
        TaskHistory::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MainQuestDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun taskDao(): TaskDao
    abstract fun rewardDao(): RewardDao
    abstract fun unlockedRewardDao(): UnlockedRewardDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun taskHistoryDao(): TaskHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: MainQuestDatabase? = null

        private val roomCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                GlobalScope.launch(Dispatchers.IO) {
                    INSTANCE?.let { database ->
                        // Dodaj korisnike
                        val userId1 = database.userDao().insert(User(username = "ana", email = "ana@mail.com", password = "lozinka1", avatar = null, xp = 100)).toInt()
                        val userId2 = database.userDao().insert(User(username = "ivan", email = "ivan@mail.com", password = "lozinka2", avatar = null, xp = 250)).toInt()
                        // Dodaj još korisnika
                        val userId3 = database.userDao().insert(User(username = "marija", email = "marija@mail.com", password = "lozinka3", avatar = null, xp = 80)).toInt()
                        val userId4 = database.userDao().insert(User(username = "marko", email = "marko@mail.com", password = "lozinka4", avatar = null, xp = 300)).toInt()
                        // Dodaj nagrade
                        val rewardId1 = database.rewardDao().insert(Reward(name = "Zlatni avatar", description = "Specijalni avatar", image = null, xpCost = 200)).toInt()
                        val rewardId2 = database.rewardDao().insert(Reward(name = "Meme skin", description = "Otključaj meme skin", image = null, xpCost = 150)).toInt()
                        // Dodaj još nagrada
                        val rewardId3 = database.rewardDao().insert(Reward(name = "Epic skin", description = "Poseban skin za profil", image = null, xpCost = 300)).toInt()
                        val rewardId4 = database.rewardDao().insert(Reward(name = "Bonus XP", description = "Dodatnih 50 XP bodova", image = null, xpCost = 100)).toInt()
                        // Dodaj zadatke
                        val taskId1 = database.taskDao().insert(Task(title = "Uči matematiku", description = "Ponovi gradivo za ispit", category = "učenje", dueDate = "2024-06-10", isCompleted = false, xpReward = 50, userId = userId1)).toInt()
                        val taskId2 = database.taskDao().insert(Task(title = "Trening", description = "Odradi 30 min vježbanja", category = "fitness", dueDate = "2024-06-11", isCompleted = true, xpReward = 30, userId = userId2)).toInt()
                        // Dodaj još zadataka
                        val taskId3 = database.taskDao().insert(Task(title = "Pročitaj knjigu", description = "Završi barem jedno poglavlje", category = "osobni razvoj", dueDate = "2024-06-12", isCompleted = false, xpReward = 40, userId = userId3)).toInt()
                        val taskId4 = database.taskDao().insert(Task(title = "Napiši izvještaj", description = "Izvještaj za projekt", category = "posao", dueDate = "2024-06-13", isCompleted = true, xpReward = 60, userId = userId4)).toInt()
                        val taskId5 = database.taskDao().insert(Task(title = "Jutarnja meditacija", description = "10 minuta meditacije", category = "zdravlje", dueDate = "2024-06-14", isCompleted = false, xpReward = 20, userId = userId1)).toInt()
                        
                        // Dodaj dnevne ciljeve za testiranje
                        database.taskDao().insert(Task(
                            title = "Pij vodu",
                            description = "Popij 8 čaša vode dnevno",
                            category = "zdravlje",
                            dueDate = null,
                            isCompleted = false,
                            xpReward = 80,
                            userId = userId1,
                            isDailyGoal = true,
                            dailyTarget = 8,
                            dailyProgress = 3,
                            lastCompletedDate = "2024-12-15",
                            streakCount = 5
                        ))
                        
                        database.taskDao().insert(Task(
                            title = "Vježbanje",
                            description = "30 minuta dnevno",
                            category = "fitness",
                            dueDate = null,
                            isCompleted = false,
                            xpReward = 50,
                            userId = userId1,
                            isDailyGoal = true,
                            dailyTarget = 1,
                            dailyProgress = 0,
                            lastCompletedDate = null,
                            streakCount = 0
                        ))
                        
                        database.taskDao().insert(Task(
                            title = "Čitanje",
                            description = "Čitaj barem 30 stranica",
                            category = "osobni razvoj",
                            dueDate = null,
                            isCompleted = false,
                            xpReward = 30,
                            userId = userId1,
                            isDailyGoal = true,
                            dailyTarget = 30,
                            dailyProgress = 15,
                            lastCompletedDate = "2024-12-15",
                            streakCount = 3
                        ))
                        // Dodaj izazove
                        val challengeId1 = database.challengeDao().insert(Challenge(name = "Tjedni izazov", description = "Završi 5 zadataka u tjednu", duration = "7 dana", reward = "100 XP")).toInt()
                        // Dodaj još izazova
                        val challengeId2 = database.challengeDao().insert(Challenge(name = "Fitness izazov", description = "Vježbaj svaki dan 5 dana", duration = "5 dana", reward = "Meme skin")).toInt()
                        val challengeId3 = database.challengeDao().insert(Challenge(name = "Čitanje izazov", description = "Pročitaj 3 knjige u mjesec dana", duration = "30 dana", reward = "Epic skin")).toInt()
                        // Dodaj otključane nagrade
                        database.unlockedRewardDao().insert(UnlockedReward(userId = userId2, rewardId = rewardId2, unlockedAt = "2024-06-09"))
                        database.unlockedRewardDao().insert(UnlockedReward(userId = userId1, rewardId = rewardId1, unlockedAt = "2024-06-10"))
                        database.unlockedRewardDao().insert(UnlockedReward(userId = userId3, rewardId = rewardId3, unlockedAt = "2024-06-12"))
                        // Dodaj povijest zadataka
                        database.taskHistoryDao().insert(TaskHistory(taskId = taskId2, userId = userId2, completedAt = "2024-06-08"))
                        database.taskHistoryDao().insert(TaskHistory(taskId = taskId4, userId = userId4, completedAt = "2024-06-13"))
                        database.taskHistoryDao().insert(TaskHistory(taskId = taskId1, userId = userId1, completedAt = "2024-06-10"))
                    }
                }
            }
        }

        fun getDatabase(context: Context): MainQuestDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MainQuestDatabase::class.java,
                    "main_quest_database"
                )
                .addCallback(roomCallback)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

private val IO_EXECUTOR = Executors.newSingleThreadExecutor()

fun ioThread(f: () -> Unit) {
    IO_EXECUTOR.execute(f)
} 