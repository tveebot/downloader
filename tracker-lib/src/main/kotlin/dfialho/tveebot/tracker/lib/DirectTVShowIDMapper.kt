package dfialho.tveebot.tracker.lib

import dfialho.tveebot.application.api.ID
import dfialho.tveebot.tracker.api.TVShowIDMapper

/**
 * Implementation of [TVShowIDMapper] that does not do any special mapping. For a given ID it returns always that same
 * value.
 *
 * @author David Fialho (dfialho@protonmail.com)
 */
class DirectTVShowIDMapper : TVShowIDMapper {

    override fun get(tvShowID: ID): String? = tvShowID.value

    /**
     * This method does nothing. It ignores any provider ID specified here.
     */
    override fun set(tvShowID: ID, providerID: String) {
    }

    override fun getTVShowID(providerID: String): ID =
        ID(providerID)
}