package net.remmintan.mods.minefortress.blocks.building

import net.minecraft.block.BedBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.FurnaceBlockEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.tag.BlockTags
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.remmintan.mods.minefortress.blocks.FortressBlocks
import net.remmintan.mods.minefortress.core.dtos.ItemInfo
import net.remmintan.mods.minefortress.core.dtos.buildings.BlueprintMetadata
import net.remmintan.mods.minefortress.core.interfaces.automation.area.IAutomationArea
import net.remmintan.mods.minefortress.core.interfaces.automation.area.IAutomationBlockInfo
import net.remmintan.mods.minefortress.core.interfaces.blueprints.ProfessionType
import net.remmintan.mods.minefortress.core.interfaces.buildings.IFortressBuilding
import net.remmintan.mods.minefortress.core.interfaces.buildings.IServerBuildingsManager
import net.remmintan.mods.minefortress.core.interfaces.professions.IProfessionsManager
import net.remmintan.mods.minefortress.core.interfaces.resources.IServerResourceManager
import net.remmintan.mods.minefortress.core.interfaces.server.IFortressServer
import net.remmintan.mods.minefortress.gui.building.BuildingScreenHandler
import java.time.LocalDateTime
import java.util.*

private const val MAX_BLOCKS_PER_UPDATE = 10

class FortressBuildingBlockEntity(pos: BlockPos?, state: BlockState?) :
    BlockEntity(FortressBlocks.BUILDING_ENT_TYPE, pos, state),
    NamedScreenHandlerFactory,
    IFortressBuilding {

    private var ownerId: UUID? = null
    private var blueprintMetadata: BlueprintMetadata? = null
    private var start: BlockPos? = null
    private var end: BlockPos? = null
    private var blockData: FortressBuildingBlockData? = null
    private var furnaceBlockPos: BlockPos? = null
    private var hireHandler: BuildingHireHandler? = null

    private var automationArea: IAutomationArea? = null

    private val attackers: MutableSet<HostileEntity> = HashSet()

    fun init(
        ownerId: UUID,
        metadata: BlueprintMetadata,
        start: BlockPos,
        end: BlockPos,
        blockData: Map<BlockPos, BlockState>,
    ) {
        this.ownerId = ownerId
        this.blueprintMetadata = metadata
        this.start = start
        this.end = end

        val movedBlocksData = blockData.mapKeys { it.key.add(start) }

        this.blockData = FortressBuildingBlockData(movedBlocksData, metadata.floorLevel)
        this.automationArea = BuildingAutomationAreaProvider(start, end, metadata.requirement)

        this.furnaceBlockPos = BlockPos.stream(start, end)
            .filter { pos -> world?.getBlockState(pos)?.block == Blocks.FURNACE }
            .map { it.toImmutable() }
            .findFirst()
            .orElse(null)

        this.hireHandler = BuildingHireHandler()
    }

    fun tick(world: World?) {
        world ?: return
        blockData?.checkTheNextBlocksState(MAX_BLOCKS_PER_UPDATE, world as? ServerWorld)

        hireHandler?.let {
            if (!it.initialized()) {
                val professionType = metadata.requirement.type ?: return@let
                val (prof, build, res) = getManagers(world)
                it.init(professionType, prof, build, res)
            }

            it.tick()
        }

        this.markDirty()
        if (this.world?.isClient == false) {
            val state = this.cachedState
            this.world?.updateListeners(this.pos, state, state, Block.NOTIFY_ALL)
        }
    }

    private fun getManagers(world: World): Triple<IProfessionsManager, IServerBuildingsManager, IServerResourceManager> {
        if (world is ServerWorld) {
            val server = world.server
            if (server is IFortressServer) {
                return server._FortressModServerManager?.getManagersProvider(ownerId)?.let {
                    Triple(it.professionsManager, it.buildingsManager, it.resourceManager)
                } ?: error("Managers provider is not set")
            }
        }
        error("Trying to get managers on the client side")
    }

    override fun getHireHandler() = hireHandler ?: error("Hire handler is not set")

    override fun getFurnace(): FurnaceBlockEntity? =
        furnaceBlockPos?.let { this.getWorld()?.let { w -> w.getBlockEntity(it) as? FurnaceBlockEntity } }

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory?, player: PlayerEntity?): ScreenHandler {
        val propertyDelegate = object : PropertyDelegate {
            override fun get(index: Int): Int {
                return when(index) {
                    0 -> pos.x
                    1 -> pos.y
                    2 -> pos.z
                    else -> error("Invalid property index")
                }
            }
            override fun set(index: Int, value: Int) {}
            override fun size(): Int = 3
        }

        return BuildingScreenHandler(syncId, propertyDelegate)
    }

    override fun getPos(): BlockPos = super<BlockEntity>.getPos()

    override fun getDisplayName(): Text {
        val nameStr = blueprintMetadata?.name ?: "Building"
        return Text.of(nameStr)
    }

    override fun destroy() {
        blockData?.allPresevedBlockPositions?.forEach {
            world?.removeBlock(it, false)
        }
    }

    override fun getMetadata(): BlueprintMetadata {
        return this.blueprintMetadata ?: error("Blueprint metadata is not set")
    }

    override fun readNbt(nbt: NbtCompound) {
        ownerId = nbt.getUuid("ownerId")
        blueprintMetadata = BlueprintMetadata(nbt.getCompound("blueprintMetadata"))
        start = BlockPos.fromLong(nbt.getLong("start"))
        end = BlockPos.fromLong(nbt.getLong("end"))
        blockData = FortressBuildingBlockData.fromNbt(nbt.getCompound("blockData"))
        automationArea = BuildingAutomationAreaProvider(start!!, end!!, blueprintMetadata!!.requirement)
        hireHandler = BuildingHireHandler.fromNbt(nbt.getCompound("hireHandler"))
    }

    override fun writeNbt(nbt: NbtCompound) {
        ownerId?.let { nbt.putUuid("ownerId", it) }
        blueprintMetadata?.toNbt()?.let { nbt.put("blueprintMetadata", it) }
        start?.let { nbt.putLong("start", it.asLong()) }
        end?.let { nbt.putLong("end", it.asLong()) }
        blockData?.toNbt()?.let { nbt.put("blockData", it) }
        hireHandler?.toNbt()?.let { nbt.put("hireHandler", it) }
    }

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> {
        val nbt = NbtCompound()
        writeNbt(nbt)
        return BlockEntityUpdateS2CPacket.create(this) { nbt }
    }

    override fun getId(): UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

    override fun getHealth(): Int = blockData?.health ?: 100

    override fun getStart(): BlockPos? = start
    override fun getEnd(): BlockPos? = end

    override fun getFreeBed(world: World?): Optional<BlockPos> {
        return BlockPos
            .stream(start, end)
            .toList()
            .firstOrNull {
                val blockState = world?.getBlockState(it) ?: Blocks.AIR.defaultState
                blockState.isIn(BlockTags.BEDS) && !blockState.get(BedBlock.OCCUPIED)
            }
            ?.let { Optional.of(it) } ?: Optional.empty()
    }


    override fun satisfiesRequirement(type: ProfessionType?, level: Int): Boolean =
        blueprintMetadata?.requirement?.satisfies(type, level) ?: false

    override fun attack(attacker: HostileEntity) {
        if (blockData?.attack(attacker) == true) attackers.add(attacker)
    }

    override fun getAttackers() = attackers
    override fun getRepairItemInfos() = getRepairStates()
        .entries
        .groupingBy { it.value.block.asItem() }
        .eachCount()
        .map { ItemInfo(it.key, it.value) }

    private fun getRepairStates() = blockData?.allBlockStatesToRepairTheBuilding ?: mapOf()

    override fun getBlocksToRepair(): Map<BlockPos, BlockState> = getRepairStates()

    // IAutomationArea
    override fun getUpdated(): LocalDateTime = automationArea?.updated ?: error("Automation area provider is not set")

    override fun update() {
        automationArea?.update()
    }

    override fun iterator(world: World?): MutableIterator<IAutomationBlockInfo> =
        automationArea?.iterator(world) ?: error("Automation area provider is not set")


}