package de.mein.auth.service

import de.mein.Lok
import de.mein.MeinRunnable
import de.mein.auth.MeinAuthAdmin
import de.mein.auth.data.MeinAuthSettings
import de.mein.auth.data.db.ServiceType
import de.mein.auth.service.power.PowerManager
import de.mein.auth.tools.BackgroundExecutor
import de.mein.auth.tools.MeinDeferredManager
import de.mein.sql.SqlQueriesException

import org.jdeferred.Promise
import org.jdeferred.impl.DeferredObject

import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.logging.Logger

/**
 * Boots up the MeinAuth instance and all existing services by calling the corresponding bootloaders.
 */
class MeinBoot(private val meinAuthSettings: MeinAuthSettings, private val powerManager: PowerManager, vararg bootloaderClasses: Class<out Bootloader<out MeinService>>) : BackgroundExecutor(), MeinRunnable {
    private val bootloaderClasses = HashSet<Class<out Bootloader<out MeinService>>>()
    private val bootloaderMap = HashMap<String, Class<out Bootloader<out MeinService>>>()
    private val deferredObject: DeferredObject<MeinAuthService, Exception, Void>
    private var meinAuthService: MeinAuthService? = null
    private val meinAuthAdmins = ArrayList<MeinAuthAdmin>()


    init {
        this.deferredObject = DeferredObject()
        if (bootloaderClasses != null)
            this.bootloaderClasses.addAll(Arrays.asList(*bootloaderClasses))
    }

    fun addMeinAuthAdmin(admin: MeinAuthAdmin): MeinBoot {
        meinAuthAdmins.add(admin)
        return this
    }


    fun addBootLoaderClass(clazz: Class<out Bootloader<MeinService>>): MeinBoot {
        bootloaderClasses.add(clazz)
        return this
    }

    fun getBootloaderMap(): Map<String, Class<out Bootloader<out MeinService>>> {
        return bootloaderMap
    }

    fun getBootloaderClasses(): Set<Class<out Bootloader<out MeinService>>> {
        return bootloaderClasses
    }


    @Throws(Exception::class)
    fun boot(): Promise<MeinAuthService, Exception, Void> {
        execute(this)
        return deferredObject
    }

    private val outstandingBootloaders = Collections.synchronizedSet(mutableSetOf<Bootloader<out MeinService>>())!!
    override fun run() {
        try {
            powerManager.addStateListener(PowerManager.IPowerStateListener {
                bootStage2()
            })
            meinAuthService = MeinAuthService(meinAuthSettings, powerManager)
            meinAuthService!!.meinBoot = this
            meinAuthService!!.addAllMeinAuthAdmin(meinAuthAdmins)
            val promiseAuthIsUp = meinAuthService!!.prepareStart()
            val bootedPromises = ArrayList<Promise<*, *, *>>()
            for (bootClass in bootloaderClasses) {
                Lok.debug("MeinBoot.boot.booting: " + bootClass.canonicalName)
                val dummyBootloader = createBootLoader(meinAuthService, bootClass)
                val services = meinAuthService!!.databaseManager.getActiveServicesByType(dummyBootloader.getTypeId())
                services.forEach { service ->
                    val bootloader = createBootLoader(meinAuthService, bootClass)
                    val booted = bootloader.bootStage1(meinAuthService, service)
                    if (booted != null) {
                        outstandingBootloaders += bootloader
                        bootedPromises.add(booted)
                        booted.fail { bootException -> outstandingBootloaders.remove(bootException.bootloader) }
                    }
                }
            }
            //bootedPromises.add(promiseAuthIsUp);
            promiseAuthIsUp.done { result -> deferredObject.resolve(meinAuthService) }
            MeinDeferredManager().`when`(bootedPromises)
                    .done {
                        // boot stage2 of all services
                        bootStage2()
                        meinAuthService!!.start()
                    }.fail { Lok.error("MeinBoot.run.AT LEAST ONE SERVICE FAILED TO BOOT") }
        } catch (e: Exception) {
            e.printStackTrace()
            deferredObject.reject(e)
        }
    }

    private fun bootStage2(){
        if (meinAuthService!!.powerManager.heavyWorkAllowed()) {
            outstandingBootloaders.forEach { bootloader ->
                bootloader.bootStage2()?.always { _, _, _ -> outstandingBootloaders.remove(bootloader) }
            }
        }
    }


    @Throws(SqlQueriesException::class, IllegalAccessException::class, InstantiationException::class)
    fun createBootLoader(meinAuthService: MeinAuthService?, bootClass: Class<out Bootloader<out MeinService>>?): Bootloader<out MeinService> {
        val bootLoader = bootClass!!.newInstance()
        bootloaderMap[bootLoader.name] = bootClass
        bootLoader.setMeinAuthService(meinAuthService)
        val databaseManager = meinAuthService!!.databaseManager
        var serviceType: ServiceType? = databaseManager.getServiceTypeByName(bootLoader.name)
        if (serviceType == null) {
            serviceType = databaseManager.createServiceType(bootLoader.name, bootLoader.description)
        }
        bootLoader.setTypeId(serviceType!!.id.v())
        val serviceTypesDir = File(meinAuthService.workingDirectory, "servicetypes")
        serviceTypesDir.mkdirs()
        val bootDir = File(serviceTypesDir, serviceType.type.v())
        bootDir.mkdirs()
        bootLoader.setBootLoaderDir(bootDir)
        return bootLoader
    }

    @Throws(IllegalAccessException::class, SqlQueriesException::class, InstantiationException::class)
    fun getBootLoader(typeName: String): Bootloader<out MeinService> {
        var bootClazz: Class<out Bootloader<out MeinService>>? = bootloaderMap[typeName]
        //todo debug
        if (bootClazz == null) {
            val hasType = bootloaderMap.containsKey(typeName)
            bootClazz = bootloaderMap[typeName]
        }
        val bootLoader = createBootLoader(meinAuthService, bootClazz)
        return bootLoader
    }

    override fun getRunnableName(): String {
        return javaClass.simpleName
    }


    override fun createExecutorService(threadFactory: ThreadFactory): ExecutorService {
        return Executors.newCachedThreadPool(threadFactory)
    }

    @Throws(SqlQueriesException::class, InstantiationException::class, IllegalAccessException::class)
    fun getBootLoader(meinService: IMeinService): Bootloader<out MeinService> {
        val typeName = meinAuthService!!.databaseManager.getServiceNameByServiceUuid(meinService.uuid)
        return getBootLoader(typeName)
    }

    companion object {
        val DEFAULT_WORKING_DIR_NAME = "mein.auth"
        val DEFAULT_SETTINGS_FILE_NAME = "auth.settings"
        private val logger = Logger.getLogger(MeinBoot::class.java.name)
        val defaultWorkingDir1 = File("meinauth.workingdir.1")
        val defaultWorkingDir2 = File("meinauth.workingdir.2")

        @JvmStatic
        fun main(args: Array<String>) {
            // MeinAuthService meinAuthService =
        }
    }
}