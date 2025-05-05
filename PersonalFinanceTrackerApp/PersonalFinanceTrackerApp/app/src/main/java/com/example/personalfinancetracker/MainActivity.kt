package com.example.personalfinancetracker

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.personalfinancetracker.data.AuthRepository
import com.example.personalfinancetracker.databinding.ActivityMainBinding
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        
        authRepository = AuthRepository(this)
        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_dashboard,
                R.id.navigation_transactions,
                R.id.navigation_profile,
                R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNavigation.setupWithNavController(navController)

        // Add a listener to handle bottom navigation visibility
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment -> {
                    // Hide bottom navigation and FAB for auth screens
                    binding.bottomAppBar.visibility = View.GONE
                    binding.fabAddTransaction.hide()
                    binding.toolbar.visibility = View.GONE
                    // Remove bottom margin from fragment container
                    binding.navHostFragment.layoutParams = (binding.navHostFragment.layoutParams as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams).apply {
                        bottomMargin = 0
                    }
                }
                else -> {
                    // Show bottom navigation and FAB for main screens
                    binding.bottomAppBar.visibility = View.VISIBLE
                    binding.fabAddTransaction.show()
                    binding.toolbar.visibility = View.VISIBLE
                    // Restore bottom margin for fragment container
                    binding.navHostFragment.layoutParams = (binding.navHostFragment.layoutParams as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams).apply {
                        bottomMargin = resources.getDimensionPixelSize(R.dimen.bottom_nav_margin)
                    }
                }
            }
        }

        // Check if user is logged in and navigate accordingly
        if (authRepository.getCurrentUser() != null) {
            navController.navigate(R.id.action_loginFragment_to_dashboardFragment)
        }

        // Set up FAB click listener
        binding.fabAddTransaction.setOnClickListener {
            // Navigate to add transaction screen
            navController.navigate(R.id.addTransactionFragment)
        }

        // Hide FAB when on certain destinations
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.addTransactionFragment -> {
                    binding.fabAddTransaction.hide()
                    binding.bottomAppBar.setFabAlignmentMode(BottomAppBar.FAB_ALIGNMENT_MODE_END)
                }
                else -> {
                    if (destination.id != R.id.loginFragment && destination.id != R.id.registerFragment) {
                        binding.fabAddTransaction.show()
                    }
                    binding.bottomAppBar.setFabAlignmentMode(BottomAppBar.FAB_ALIGNMENT_MODE_CENTER)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}