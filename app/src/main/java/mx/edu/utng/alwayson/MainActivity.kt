package mx.edu.utng.alwayson

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.wear.ambient.AmbientModeSupport
import mx.edu.utng.alwayson.databinding.ActivityMainBinding
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {

    private lateinit var binding: ActivityMainBinding

    /**
     * El controlador de modo añadido para esta pantalla. Usada por la actividad
     * para ver si el está en ambiente.
     */
    private var mAmbientController: AmbientModeSupport.AmbientController? = null

    /**
     *
     *
     */
    private var mIsLowBitAmbient = false

    /**
     *
     *
     */

    private var mDoBurnInProtection = false
    private var mContentView: View? = null
    private var mTimeTextView: TextView? = null
    private var mTimeStampTextView: TextView? = null
    private var mStateTextView: TextView? = null
    private var mUpdateRateTextView: TextView? = null
    private var mDrawCountTextView: TextView? = null
    private val sDateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    @Volatile
    private var mDrawCount = 0

    /**
     *
     *
     *
     *
     *
     *
     */

    private var mAmbientUpdateAlarmManager: AlarmManager? = null
    private var mAmbientUpdatePendingIntent: PendingIntent? = null
    private var mAmbientUpdateBroadcastReceiver: BroadcastReceiver? = null

    /**
     *
     *
     */

    private val mActiveModeUpdateHandler: Handler = ActiveModeUpdateHandler(this)

    /** */
    companion object{
        private const val TAG = "MainActivity"
        /** */
        private const val MSG_UPDATE_SCREEN = 0
        /** */
        private val ACTIVE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1)
        private val AMBIENT_INTERVAL_MS = TimeUnit.SECONDS.toMillis(10)
        /**
         */
        private const val AMBIENT_UPDATE_ACTION = "mx.edu.alwayson.action.AMBIENT_UPDATE"
        /**
         */
        const val BURN_IN_OFFSET_PX = 10
    } //Fin companion object

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate()")
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAmbientController = AmbientModeSupport.attach(this)
        mAmbientUpdateAlarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        /**
         *
         *
         */
        val ambientUpdateIntent = Intent(AMBIENT_UPDATE_ACTION)

        /**
         *
         *
         *
         *
         *
         *
         *
         *
         *
         *
         *
         */
        mAmbientUpdatePendingIntent = PendingIntent.getBroadcast(
            this, 0, ambientUpdateIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        /**
         *
         *
         */
        mAmbientUpdateBroadcastReceiver = object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                refreshDisplayAndSetNextUpdate()
            }
        }
        mContentView = findViewById(R.id.content_view)
        mTimeTextView = findViewById(R.id.time)
        mTimeStampTextView = findViewById(R.id.time_stamp)
        mStateTextView = findViewById(R.id.state)
        mUpdateRateTextView = findViewById(R.id.update_rate)
        mDrawCountTextView = findViewById(R.id.draw_count)

    }

    override fun onResume() {
        Log.d(TAG, "onResume()")
        super.onResume()
        val filter = IntentFilter(AMBIENT_UPDATE_ACTION)
        registerReceiver(mAmbientUpdateBroadcastReceiver, filter)
        refreshDisplayAndSetNextUpdate()

    }

    override fun onPause() {
        Log.d(TAG, "onPause()")
        super.onPause()
        unregisterReceiver(mAmbientUpdateBroadcastReceiver)
        mActiveModeUpdateHandler.removeMessages(MSG_UPDATE_SCREEN)
        mAmbientUpdateAlarmManager!!.cancel(mAmbientUpdatePendingIntent)
    }

    /**
     *
     *
     */
    private fun refreshDisplayAndSetNextUpdate() {
        loadDataAndUpdateScreen()
        val timeMs = System.currentTimeMillis()
        if (mAmbientController!!.isAmbient){
            /* */
            val delayMs = AMBIENT_INTERVAL_MS -timeMs % AMBIENT_INTERVAL_MS
            val triggerTimeMs = timeMs + delayMs
            mAmbientUpdateAlarmManager!!.setExact(
                AlarmManager.RTC_WAKEUP, triggerTimeMs, mAmbientUpdatePendingIntent
            )
        }else{
            /* */
            val delayMs = ACTIVE_INTERVAL_MS -timeMs % ACTIVE_INTERVAL_MS
            mActiveModeUpdateHandler.removeMessages(MSG_UPDATE_SCREEN)
            mActiveModeUpdateHandler.sendEmptyMessageDelayed(MSG_UPDATE_SCREEN, delayMs)
        }
    }

    /*
     */
    private fun loadDataAndUpdateScreen() {
        mDrawCount+=1
        val currentTimeMs = System.currentTimeMillis()
        Log.d(
            TAG,
            "loadDataAndUpdateScreen() "+currentTimeMs+"("+mAmbientController!!.isAmbient +")")
        if (mAmbientController!!.isAmbient){
            mTimeTextView!!.text = sDateFormat.format(Date())
            mTimeStampTextView!!.text = getString(R.string.timestamp_label, currentTimeMs)
            mStateTextView!!.text = getString(R.string.mode_ammbient_label)
            mUpdateRateTextView!!.text =
                getString(R.string.update_rate_label, AMBIENT_INTERVAL_MS / 1000)
            mDrawCountTextView!!.text = getString(R.string.draw_count_label, mDrawCount)
        }
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback {
        return MyAmbientCallback()
    }

    /*
     */
    private inner class MyAmbientCallback : AmbientModeSupport.AmbientCallback() {
        override fun onEnterAmbient(ambientDetails: Bundle?) {
            super.onEnterAmbient(ambientDetails)
            mIsLowBitAmbient =
                ambientDetails!!.getBoolean(AmbientModeSupport.EXTRA_LOWBIT_AMBIENT, false)
            mDoBurnInProtection =
                ambientDetails!!.getBoolean(AmbientModeSupport.EXTRA_BURN_IN_PROTECTION, false)
            /* */
            mActiveModeUpdateHandler.removeMessages(MSG_UPDATE_SCREEN)
            /**
             *
             *
             */
            mStateTextView!!.setTextColor(Color.WHITE)
            mUpdateRateTextView!!.setTextColor(Color.WHITE)
            mDrawCountTextView!!.setTextColor(Color.WHITE)
            if (mIsLowBitAmbient){
                mTimeTextView!!.paint.isAntiAlias = false
                mTimeStampTextView!!.paint.isAntiAlias = false
                mStateTextView!!.paint.isAntiAlias = false
                mUpdateRateTextView!!.paint.isAntiAlias = false
                mDrawCountTextView!!.paint.isAntiAlias = false
            }
            refreshDisplayAndSetNextUpdate()
        }

        override fun onUpdateAmbient() {
            super.onUpdateAmbient()
            /**
             *
             *
             *
             *
             *
             *
             *
             *
             */
            if (mDoBurnInProtection){
                val x = (Math.random() * 2 * BURN_IN_OFFSET_PX - BURN_IN_OFFSET_PX).toInt()
                val y = (Math.random() * 2 * BURN_IN_OFFSET_PX - BURN_IN_OFFSET_PX).toInt()
                mContentView!!.setPadding(x, y, 0, 0)
            }
        }

        override fun onExitAmbient() {
            super.onExitAmbient()
            /**
             *
             */
            mAmbientUpdateAlarmManager!!.cancel(mAmbientUpdatePendingIntent)
            mStateTextView!!.setTextColor(Color.GREEN)
            mUpdateRateTextView!!.setTextColor(Color.GREEN)
            mDrawCountTextView!!.setTextColor(Color.GREEN)
            if (mIsLowBitAmbient){
                mTimeTextView!!.paint.isAntiAlias = true
                mTimeStampTextView!!.paint.isAntiAlias = true
                mStateTextView!!.paint.isAntiAlias = true
                mUpdateRateTextView!!.paint.isAntiAlias = true
                mDrawCountTextView!!.paint.isAntiAlias = true
            }
            /*
             *
             */
            if (mDoBurnInProtection){
                mContentView!!.setPadding(0, 0, 0, 0)
            }
            refreshDisplayAndSetNextUpdate()
        }
    }
    /**
     *
     */
    private class ActiveModeUpdateHandler internal constructor(
        reference : MainActivity) : Handler() {
        private val mMainActivityWeakReference : WeakReference<MainActivity>
        override fun handleMessage(msg: Message) {
            val mainActivity = mMainActivityWeakReference.get()
            if (mainActivity!=null){
                mainActivity.refreshDisplayAndSetNextUpdate()
            }
        }
        init {
            mMainActivityWeakReference = WeakReference(reference)
        }
    }
}