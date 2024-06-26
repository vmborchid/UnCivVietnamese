package com.unciv.logic.civilization.managers

import com.unciv.logic.IsPartOfGameInfoSerialization
import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.tile.Tile
import com.unciv.models.Spy


class EspionageManager : IsPartOfGameInfoSerialization {

    var spyList = ArrayList<Spy>()
    val erasSpyEarnedFor = LinkedHashSet<String>()

    @Transient
    lateinit var civInfo: Civilization

    fun clone(): EspionageManager {
        val toReturn = EspionageManager()
        spyList.mapTo(toReturn.spyList) { it.clone() }
        toReturn.erasSpyEarnedFor.addAll(erasSpyEarnedFor)
        return toReturn
    }

    fun setTransients(civInfo: Civilization) {
        this.civInfo = civInfo
        for (spy in spyList) {
            spy.setTransients(civInfo)
        }
    }

    fun endTurn() {
        for (spy in spyList.toList())
            spy.endTurn()
    }

    fun getSpyName(): String {
        val usedSpyNames = spyList.map { it.name }.toHashSet()
        val validSpyNames = civInfo.nation.spyNames.filter { it !in usedSpyNames }
        if (validSpyNames.isEmpty()) { return "Spy ${spyList.size+1}" } // +1 as non-programmers count from 1
        return validSpyNames.random()
    }

    fun addSpy(): Spy {
        val spyName = getSpyName()
        val newSpy = Spy(spyName)
        newSpy.setTransients(civInfo)
        spyList.add(newSpy)
        return newSpy
    }

    fun getTilesVisibleViaSpies(): Sequence<Tile> {
        return spyList.asSequence()
            .filter { it.isSetUp() }
            .mapNotNull { it.getLocation() }
            .flatMap { it.getCenterTile().getTilesInDistance(1) }
    }

    fun getTechsToSteal(otherCiv: Civilization): Set<String> {
        val techsToSteal = mutableSetOf<String>()
        for (tech in otherCiv.tech.techsResearched) {
            if (civInfo.tech.isResearched(tech)) continue
            if (!civInfo.tech.canBeResearched(tech)) continue
            techsToSteal.add(tech)
        }
        return techsToSteal
    }

    fun getSpiesInCity(city: City): MutableList<Spy> {
        return spyList.filter { it.getLocation() == city }.toMutableList()
    }

    /**
     * Returns a list of all cities with our spies in them.
     * The list needs to be stable accross calls on the same turn.
     */
    fun getCitiesWithOurSpies(): List<City> = spyList.filter { it.isSetUp() }.mapNotNull { it.getLocation() }

    fun getSpyAssignedToCity(city: City): Spy? = spyList.firstOrNull {it.getLocation() == city}
}
