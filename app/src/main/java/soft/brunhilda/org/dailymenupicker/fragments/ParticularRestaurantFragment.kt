package soft.brunhilda.org.dailymenupicker.fragments

import android.content.pm.PackageManager
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import io.nlopez.smartlocation.SmartLocation
import io.nlopez.smartlocation.location.config.LocationParams
import kotlinx.android.synthetic.main.list_days.*
import soft.brunhilda.org.dailymenupicker.ComparablePlace
import soft.brunhilda.org.dailymenupicker.R
import soft.brunhilda.org.dailymenupicker.adapters.FoodEntityAdapter
import soft.brunhilda.org.dailymenupicker.database.DatabaseManager
import soft.brunhilda.org.dailymenupicker.entity.DayOfWeek
import soft.brunhilda.org.dailymenupicker.entity.RestaurantWeekData
import soft.brunhilda.org.dailymenupicker.resolvers.CachedRestDataResolver
import soft.brunhilda.org.dailymenupicker.transformers.FoodAdapterTransformer

class ParticularRestaurantFragment : ParentFragment(), OnMapReadyCallback {
    private lateinit var place: ComparablePlace
    private lateinit var mapView: MapView

    private val dataResolver = CachedRestDataResolver()
    private val dataTransformer = FoodAdapterTransformer.getInstance()

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val databaseManager = DatabaseManager(context)
        place = this.arguments.getSerializable("googlePlace") as ComparablePlace

        val myFab = view?.findViewById(R.id.fab) as FloatingActionButton
        setUpFavouriteButton(databaseManager, myFab, view)

        placesPreparationIsFinished(mutableSetOf(place))
    }

    private fun setUpFavouriteButton(databaseManager: DatabaseManager, myFab: FloatingActionButton, view: View) {
        if (databaseManager.isPlaceInDb(place.placeId)) {
            myFab.setImageResource(R.drawable.ic_dislike)
        } else {
            myFab.setImageResource(R.drawable.ic_like)
        }
        myFab.setOnClickListener(ButtonManager().addToFavourite(databaseManager, myFab, place))
    }

    private fun placesPreparationIsFinished(places: Set<ComparablePlace>) {
        dataResolver.resolvePlaces(places.toList(), this::placesResolvingIsFinished)
    }

    private fun placesResolvingIsFinished(places: Map<ComparablePlace, RestaurantWeekData?>) {
        if (context != null) {
            val weekData = places.values.first() ?: return
            setDataForDate(day_view_monday, monday_no_food_message, weekData, DayOfWeek.MONDAY)
            setDataForDate(day_view_tuesday, tuesday_no_food_message, weekData, DayOfWeek.TUESDAY)
            setDataForDate(day_view_wednesday, wednesday_no_food_message, weekData, DayOfWeek.WEDNESDAY)
            setDataForDate(day_view_thursday, thursday_no_food_message, weekData, DayOfWeek.THURSDAY)
            setDataForDate(day_view_friday, friday_no_food_message, weekData, DayOfWeek.FRIDAY)
        }
    }

    private fun setDataForDate(view: RecyclerView, noFoodView: ConstraintLayout, restaurantWeekData: RestaurantWeekData, dayOfWeek: DayOfWeek){
        val dailyData = restaurantWeekData.findMenuForDay(dayOfWeek)
        if(dailyData!=null){
            view.layoutManager = LinearLayoutManager(context, LinearLayout.VERTICAL, false)
            view.adapter = FoodEntityAdapter(
                    dataTransformer.transform(place, dailyData, dayOfWeek),
                    ButtonManager().agendaAddButton(context, dayOfWeek),
                    ButtonManager().agendaAddButtonLayout())

            view.visibility = View.VISIBLE
            noFoodView.visibility = View.GONE
        } else {
            noFoodView.visibility = View.VISIBLE
            view.visibility = View.GONE
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater?.inflate(R.layout.content_particular_restaurant, container, false)

        mapView = view?.findViewById(R.id.mapwhere) as MapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {

        SmartLocation.with(context).location()
                .config(LocationParams.BEST_EFFORT)
                .start({})

        val location = SmartLocation.with(context).location().lastLocation


            val mapManager = MapViewManager(activity, context, googleMap, place)
            if (mapManager.checkPermission())
                mapManager.createMap(location)
            else
                requestPermissions(
                        arrayOf(
                                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                        ), 1)
    }

    override fun onResume() {
        super.onResume()
        activity.title = place.name
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults.filter { it==PackageManager.PERMISSION_DENIED }.isNotEmpty()){
            System.err.println("Permission was denied, permission: $permissions")
            fragmentManager.popBackStackImmediate() //sorry, get back
        }else{
            val mapView = activity.findViewById(R.id.mapwhere) as MapView
            mapView.getMapAsync(this)
        }
    }
}