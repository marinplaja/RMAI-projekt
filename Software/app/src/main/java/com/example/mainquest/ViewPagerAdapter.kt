package com.example.mainquest

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> TasksFragment.newInstance()
            1 -> ProgressFragment.newInstance()
            2 -> StatisticsFragment.newInstance()
            3 -> ProfileFragment.newInstance()
            4 -> CompletedHabitsFragment()
            else -> TasksFragment.newInstance()
        }
    }
} 