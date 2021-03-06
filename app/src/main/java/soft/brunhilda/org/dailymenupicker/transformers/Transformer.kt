package soft.brunhilda.org.dailymenupicker.transformers

import soft.brunhilda.org.dailymenupicker.ComparablePlace
import soft.brunhilda.org.dailymenupicker.entity.RestaurantWeekData

interface Transformer<T> {
    fun transform(from: Map<ComparablePlace, RestaurantWeekData?>): MutableList<T>
}