package net.perfectdreams.dreamreparar

import com.okkero.skedule.schedule
import net.perfectdreams.dreamcore.utils.KotlinPlugin
import net.perfectdreams.dreamcore.utils.extensions.hasStoredMetadataWithKey
import net.perfectdreams.dreamcore.utils.registerEvents
import net.perfectdreams.dreamcore.utils.scheduler
import net.perfectdreams.dreamreparar.listeners.SignListener
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import java.util.*

class DreamReparar : KotlinPlugin(), Listener {
	companion object {
		lateinit var INSTANCE: DreamReparar
	}

	override fun softEnable() {
		super.softEnable()

		INSTANCE = this

		registerEvents(SignListener(this))
		programarPlaca()
	}

	fun programarPlaca() {
		scheduler().schedule(this) {
			while (true) {
				for (p in Bukkit.getOnlinePlayers()) {
					try {
						val targetBlock = p.getTargetBlock(null as HashSet<Material>?, 5)
						if ((targetBlock.type != Material.SIGN && targetBlock.type != Material.WALL_SIGN) || (targetBlock.state as Sign).getLine(0) != "§1[Reparar]") {
							continue
						}

						if (p.inventory.itemInMainHand != null && p.inventory.itemInMainHand.type != Material.AIR) {
							val itemReparar = p.inventory.itemInMainHand

							val meta = itemReparar.itemMeta

							if (meta is Damageable) {
								val durability = meta.damage

								if (durability != 0) {
									val lines = (targetBlock.state as Sign).lines
									lines[1] = "Grana:"
									lines[2] = "${calculatePrice(itemReparar, p)}$"
									var desconto = ""
									if (p.hasPermission("dreamreparar.vip")) {
										desconto = "10% OFF"
									}
									if (p.hasPermission("dreamreparar.vip+")) {
										desconto = "20% OFF"
									}
									if (p.hasPermission("dreamreparar.vip++")) {
										desconto = "30% OFF"
									}
									lines[3] = Math.round(calculatePercentage(itemReparar, true)).toString() + "% " + desconto
									p.sendSignChange(targetBlock.location, lines)
								} else {
									val lines = (targetBlock.state as Sign).lines
									lines[1] = "Não é"
									lines[2] = "necessário"
									lines[3] = "reparar isto!"
									p.sendSignChange(targetBlock.location, lines)
								}
							} else {
								val lines = (targetBlock.state as Sign).lines
								lines[1] = "Não é"
								lines[2] = "possível"
								lines[3] = "reparar isto!"
								p.sendSignChange(targetBlock.location, lines)
							}
						} else {
							val lines2 = (targetBlock.state as Sign).lines
							lines2[1] = "Você não pode"
							lines2[2] = "reparar a sua"
							lines2[3] = "mão!"
							p.sendSignChange(targetBlock.location, lines2)
						}
					} catch (ex: IllegalStateException) {
					}
				}

				waitFor(20)
			}
		}
	}

	fun calculatePercentage(itemStack: ItemStack, toMulti: Boolean): Float {
		val meta = itemStack.itemMeta

		if (meta is Damageable) {
			val durability = meta.damage

			val maxDurability = itemStack.type.maxDurability.toFloat()
			var reparar = durability / maxDurability
			if (toMulti) {
				reparar = durability / maxDurability * 100.0f
			}
			return reparar
		}

		return 0f
	}

	fun calculatePrice(`is`: ItemStack, p: Player): Float {
		val price = calculatePercentage(`is`, false)
		var basePrice = 125.0f
		if (`is`.hasStoredMetadataWithKey("isMonsterPickaxe")) {
			basePrice *= 32.0f
		} else if (`is`.type.name.contains("STONE")) {
			basePrice *= 4.0f
		} else if (`is`.type.name.contains("GOLD")) {
			basePrice *= 8.0f
		} else if (`is`.type.name.contains("IRON")) {
			basePrice *= 16.0f
		} else if (`is`.type == Material.ELYTRA && `is`.type.name.contains("DIAMOND")) {
			basePrice *= 24.0f
		}
		for ((_, value) in `is`.enchantments) {
			if (value > 1) {
				basePrice *= (value / 2).toFloat()
			}
		}
		var desconto = false
		if (p.hasPermission("dreamreparar.vip") && !desconto) {
			basePrice *= 0.3f
			desconto = true
		} else if (p.hasPermission("dreamreparar.vip+") && !desconto) {
			basePrice *= 0.2f
			desconto = true
		} else if (p.hasPermission("dreamreparar.vip++") && !desconto) {
			basePrice *= 0.1f
			desconto = true
		}
		return Math.round(price * basePrice).toFloat()
	}

	fun canRepair(`is`: ItemStack): Boolean {
		return `is`.type.name.contains("SWORD") || `is`.type.name.contains("AXE") || `is`.type.name.contains("HOE") || `is`.type.name.contains("SHOVEL") || `is`.type.name.contains("HOE") || `is`.type.name.contains("PICKAXE") || `is`.type.name.contains("CHESTPLATE") || `is`.type.name.contains("BOOTS") || `is`.type.name.contains("HELMET") || `is`.type.name.contains("BARDING") || `is`.type.name.contains("LEGGINGS") || `is`.type == Material.FISHING_ROD || `is`.type == Material.BOW || `is`.type == Material.FLINT_AND_STEEL || `is`.type == Material.SHEARS || `is`.type == Material.SHIELD || `is`.type == Material.ELYTRA
	}
}