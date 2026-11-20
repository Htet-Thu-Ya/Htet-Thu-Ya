package com.example.data.betting

import com.example.data.model.BetOrder
import com.example.data.model.UserAccount
import java.util.Calendar

enum class SettlementRange(val label: String) {
    TODAY("ယနေ့"),
    LAST_7_DAYS("၇ ရက်"),
    ALL("အားလုံး")
}

data class CustomerSettlementSummary(
    val userId: String,
    val customerName: String,
    val email: String,
    val balanceKs: Long,
    val rewardPoints: Long,
    val wonCount: Int,
    val lostCount: Int,
    val pushRejectedCount: Int,
    val pendingCount: Int,
    val settledStakeKs: Long,
    val totalWonPayoutKs: Long,
    val totalReturnedKs: Long,
    val totalLostStakeKs: Long,
    val totalCommissionKs: Long,
    val pendingStakeKs: Long,
    val customerNetKs: Long,
    val adminNetKs: Long,
    val orders: List<BetOrder>
)

data class AdminSettlementSummary(
    val totalSettledOrders: Int,
    val wonCount: Int,
    val lostCount: Int,
    val pushRejectedCount: Int,
    val pendingCount: Int,
    val totalSettledStakeKs: Long,
    val totalPayoutKs: Long,
    val totalLostKeptKs: Long,
    val totalWonNetPaidKs: Long,
    val totalCommissionKs: Long,
    val totalPendingStakeKs: Long,
    val adminNetKs: Long,
    val customerSummaries: List<CustomerSettlementSummary>
)

object SettlementCalculator {

    fun buildReport(
        orders: List<BetOrder>,
        usersById: Map<String, UserAccount>,
        range: SettlementRange,
        nowMillis: Long = System.currentTimeMillis()
    ): AdminSettlementSummary {
        val scopedOrders = orders.filter { isOrderInRange(it, range, nowMillis) }
        val grouped = scopedOrders.groupBy { it.userId }

        val customerSummaries = grouped.map { (uid, custOrders) ->
            val user = usersById[uid]
            summarizeCustomer(uid, user, custOrders)
        }.sortedWith(
            compareByDescending<CustomerSettlementSummary> { it.pendingCount > 0 }
                .thenByDescending { kotlin.math.abs(it.customerNetKs) }
                .thenByDescending { it.settledStakeKs + it.pendingStakeKs }
        )

        var wonCount = 0
        var lostCount = 0
        var pushRejectedCount = 0
        var pendingCount = 0
        var totalSettledStakeKs = 0L
        var totalPayoutKs = 0L
        var totalLostKeptKs = 0L
        var totalWonNetPaidKs = 0L
        var totalCommissionKs = 0L
        var totalPendingStakeKs = 0L

        customerSummaries.forEach { cs ->
            wonCount += cs.wonCount
            lostCount += cs.lostCount
            pushRejectedCount += cs.pushRejectedCount
            pendingCount += cs.pendingCount
            totalSettledStakeKs += cs.settledStakeKs
            totalPayoutKs += cs.totalReturnedKs
            totalLostKeptKs += cs.totalLostStakeKs
            totalCommissionKs += cs.totalCommissionKs
            totalPendingStakeKs += cs.pendingStakeKs
        }

        // Exact winning profit paid out above stake on won orders
        scopedOrders.filter { it.status == "won" }.forEach { o ->
            totalWonNetPaidKs += (o.actualPayoutKs - o.stakeKs)
        }

        val adminNetKs = totalSettledStakeKs - totalPayoutKs

        return AdminSettlementSummary(
            totalSettledOrders = wonCount + lostCount,
            wonCount = wonCount,
            lostCount = lostCount,
            pushRejectedCount = pushRejectedCount,
            pendingCount = pendingCount,
            totalSettledStakeKs = totalSettledStakeKs,
            totalPayoutKs = totalPayoutKs,
            totalLostKeptKs = totalLostKeptKs,
            totalWonNetPaidKs = totalWonNetPaidKs,
            totalCommissionKs = totalCommissionKs,
            totalPendingStakeKs = totalPendingStakeKs,
            adminNetKs = adminNetKs,
            customerSummaries = customerSummaries
        )
    }

    fun summarizeCustomer(
        userId: String,
        user: UserAccount?,
        orders: List<BetOrder>
    ): CustomerSettlementSummary {
        var wonCount = 0
        var lostCount = 0
        var pushRejectedCount = 0
        var pendingCount = 0
        var settledStakeKs = 0L
        var totalWonPayoutKs = 0L
        var totalReturnedKs = 0L
        var totalLostStakeKs = 0L
        var totalCommissionKs = 0L
        var pendingStakeKs = 0L

        orders.forEach { order ->
            when (order.status) {
                "won" -> {
                    wonCount++
                    settledStakeKs += order.stakeKs
                    totalWonPayoutKs += order.actualPayoutKs
                    totalReturnedKs += order.actualPayoutKs
                    totalCommissionKs += order.commissionKs
                }
                "lost" -> {
                    lostCount++
                    settledStakeKs += order.stakeKs
                    totalReturnedKs += order.actualPayoutKs
                    totalLostStakeKs += (order.stakeKs - order.actualPayoutKs).coerceAtLeast(0L)
                }
                "rejected" -> {
                    pushRejectedCount++
                }
                "pending", "accepted" -> {
                    pendingCount++
                    pendingStakeKs += order.stakeKs
                }
            }
        }

        val customerNetKs = totalReturnedKs - settledStakeKs
        val adminNetKs = -customerNetKs

        return CustomerSettlementSummary(
            userId = userId,
            customerName = user?.displayName?.ifBlank { null }
                ?: orders.firstOrNull()?.customerName?.ifBlank { null }
                ?: "Member",
            email = user?.email.orEmpty(),
            balanceKs = user?.balanceKs ?: 0L,
            rewardPoints = user?.rewardPoints ?: 0L,
            wonCount = wonCount,
            lostCount = lostCount,
            pushRejectedCount = pushRejectedCount,
            pendingCount = pendingCount,
            settledStakeKs = settledStakeKs,
            totalWonPayoutKs = totalWonPayoutKs,
            totalReturnedKs = totalReturnedKs,
            totalLostStakeKs = totalLostStakeKs,
            totalCommissionKs = totalCommissionKs,
            pendingStakeKs = pendingStakeKs,
            customerNetKs = customerNetKs,
            adminNetKs = adminNetKs,
            orders = orders
        )
    }

    private fun isOrderInRange(order: BetOrder, range: SettlementRange, nowMillis: Long): Boolean {
        if (range == SettlementRange.ALL) return true
        val refTs = if (order.status in listOf("won", "lost", "rejected")) {
            order.updatedAt ?: order.createdAt
        } else {
            order.createdAt ?: order.updatedAt
        } ?: return true // optimistic local pending write counts as today

        val refMillis = refTs.toDate().time
        return when (range) {
            SettlementRange.TODAY -> isSameLocalDay(refMillis, nowMillis)
            SettlementRange.LAST_7_DAYS -> (nowMillis - refMillis) <= 7L * 24L * 60L * 60L * 1000L
            SettlementRange.ALL -> true
        }
    }

    private fun isSameLocalDay(aMillis: Long, bMillis: Long): Boolean {
        val calA = Calendar.getInstance().apply { timeInMillis = aMillis }
        val calB = Calendar.getInstance().apply { timeInMillis = bMillis }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }
}
