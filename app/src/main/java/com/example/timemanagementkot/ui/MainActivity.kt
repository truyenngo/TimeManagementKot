package com.example.timemanagementkot.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.timemanagementkot.data.model.ActivityModel
import com.example.timemanagementkot.data.model.ActivityWithLogTime
import com.example.timemanagementkot.data.model.GoalHeader
import com.example.timemanagementkot.data.model.GoalWithDetail
import com.example.timemanagementkot.ui.list_activity.ListActivityScreen
import com.example.timemanagementkot.ui.add_activity.AddActivityScreen
import com.example.timemanagementkot.ui.home.HomeScreen
import com.example.timemanagementkot.ui.theme.TimeManagementKotTheme
import com.example.timemanagementkot.ui.register.RegisterScreen
import com.example.timemanagementkot.ui.login.LoginScreen
import com.example.timemanagementkot.ui.detail_activity.DetailActivityScreen
import com.example.timemanagementkot.ui.detail_goal.DetailGoalScreen
import com.example.timemanagementkot.ui.goal.GoalScreen
import com.example.timemanagementkot.ui.log_time.LogTimeScreen
import com.example.timemanagementkot.ui.setting.SettingScreen
import com.example.timemanagementkot.ui.statistic.StatisticsScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimeManagementKotTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        composable("register") {
                            RegisterScreen(
                                onRegister = { email, password ->
                                    navController.navigate("login")
                                },
                                onNavigateToLogin = {
                                    navController.navigate("login")
                                }
                            )
                        }
                        composable("login") {
                            LoginScreen(
                                onLogin = { email, password ->
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                },
                                onGoogleSignInClick = {
                                }
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                onActivityListClick = {
                                    navController.navigate("listActivity")
                                },
                                onTargetClick = {
                                    navController.navigate("goal")
                                },
                                onStatisticClick = {
                                    navController.navigate("statistic")
                                },
                                onSettingClick = {
                                    navController.navigate("setting")
                                },
                                onActivityClick = { activityWithLog ->
                                    val activityWithLogJson = activityWithLog.toJson()
                                    navController.navigate("logTime/$activityWithLogJson")
                                }
                            )
                        }
                        composable("listActivity") {
                            ListActivityScreen(
                                onActivityClick = { activity ->
                                    val activityJson = activity.toJson()
                                    navController.navigate("detailActivity/$activityJson")
                                },
                                onAddActivityClick = {
                                    navController.navigate("addActivity")
                                },
                                onDeleteClick = {

                                },
                                onBack = {
                                    navController.navigate("home")
                                }
                            )
                        }
                        composable("addActivity") {
                            AddActivityScreen(
                                onSave = {
                                    navController.navigate("listActivity")
                                },
                                onNavigationToList = {
                                    navController.navigate("listActivity")
                                }
                            )
                        }
                        composable("detailActivity/{activityJson}") { backStackEntry ->
                            val activityJson = backStackEntry.arguments?.getString("activityJson") ?: ""

                            val activity = ActivityModel.fromJson(activityJson)

                            DetailActivityScreen(
                                activity = activity,
                                onUpdateClick = {

                                },
                                onNavigationToList = {
                                    navController.navigate("listActivity")
                                }
                            )
                        }
                        composable("logTime/{activityWithLogJson}") { backStackEntry ->
                            val activityWithLogJson = backStackEntry.arguments?.getString("activityWithLogJson") ?: ""

                            Log.e("LogTimeScreen", "Received activityWithLogJson: $activityWithLogJson")

                            val activityWithLog = ActivityWithLogTime.fromJson(activityWithLogJson)

                            LogTimeScreen(
                                activityWithLogJson = activityWithLog,
                                onBack = {
                                    navController.navigate("home")
                                },
                                onComplete = {
                                    navController.navigate("home")
                                }
                            )
                        }
                        composable("goal") {
                            GoalScreen(
                                onGoalClick = { goal ->
                                    val goalJson = goal.toJson()
                                    navController.navigate("detailGoal/$goalJson")
                                },
                                onAddGoalClick = {

                                },
                                onDeleteClick = {

                                },
                                onBack = {
                                    navController.navigate("home")
                                }
                            )
                        }
                        composable("detailGoal/{goalJson}") { backStackEntry ->
                            val goalJson = backStackEntry.arguments?.getString("goalJson") ?: ""
                            val goal = GoalWithDetail.fromJson(goalJson)

                            DetailGoalScreen(
                                goal = goal,
                                onSaveClick = { navController.navigate("goal") },
                                onNavigationToList = { navController.navigate("goal") }
                            )
                        }
                        composable("setting") {
                            SettingScreen(
                                onBackClick = {
                                    navController.navigate("home")
                                },
                                onChangePasswordClick= {

                                },
                                onLogoutClick = {
                                    navController.navigate("login")
                                }
                            )
                        }
                        composable("statistic") {
                            StatisticsScreen(
                                onBack = {
                                    navController.navigate("home")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
