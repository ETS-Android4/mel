package de.mel.drive.test;


import static org.junit.Assert.assertEquals;

/**
 * Created by xor on 09.09.2016.
 */
@SuppressWarnings("Duplicates")
public class DriveFXTest {

//    private static MelStandAloneAuthFX standAloneAuth2;
//    private static MelStandAloneAuthFX standAloneAuth1;
//    private static RWLock lock = new RWLock();
//
//    @Test
//    public void conflict() throws Exception {
////        DriveTest driveTest = new DriveTest();
////        MelBoot melBoot = new MelBoot(new DriveTest().createJson2(), DriveFXBootloader.class).addMelAuthAdmin(new MelAuthFxLoader());
////        MelBoot restartMelBoot = new MelBoot(new DriveTest().createJson2(), DriveFXBootloader.class).addMelAuthAdmin(new MelAuthFxLoader());
////        driveTest.clientConflictImpl(melBoot, null);
////        new WaitLock().lock().lock();
//    }
//
//    @Test
//    public void startUpConflicts() throws Exception {
////        DriveTest driveTest = new DriveTest();
////        MelBoot melBoot = new MelBoot(driveTest.createJson2(), DriveFXBootloader.class).addMelAuthAdmin(new MelAuthFxLoader());
////        driveTest.startUpConflicts(melBoot);
////        new WaitLock().lock().lock();
//    }
//
//    @Test
//    public void complexConflict() throws Exception {
////        DriveTest driveTest = new DriveTest();
////        MelBoot melBoot = new MelBoot(new DriveTest().createJson2(), DriveFXBootloader.class).addMelAuthAdmin(new MelAuthFxLoader());
////
////        MelBoot restartMelBoot = new MelBoot(new DriveTest().createJson2(), DriveFXBootloader.class).addMelAuthAdmin(new MelAuthFxLoader());
////        driveTest.complexClientConflictImpl(melBoot, null);
////        new WaitLock().lock().lock();
//    }
//
//    @Test
//    public void startEmptyClient() throws Exception {
//        //CertificateManager.deleteDirectory(MelBoot.defaultWorkingDir1);
//        CertificateManager.deleteDirectory(MelBoot.defaultWorkingDir2);
//        N runner = new N(e -> e.printStackTrace());
//        MelAuthSettings json1 = new MelAuthSettings().setPort(8890).setDeliveryPort(8891)
//                .setBrotcastListenerPort(6699).setBrotcastPort(9966)
//                .setWorkingDirectory(MelBoot.defaultWorkingDir2).setName("Test Client").setGreeting("greeting2");
//        IRegisterHandler allowRegisterHandler = new IRegisterHandler() {
//            @Override
//            public void acceptCertificate(IRegisterHandlerListener listener, MelRequest request, Certificate myCertificate, Certificate certificate) {
//                listener.onCertificateAccepted(request, certificate);
//            }
//
//            @Override
//            public void onRegistrationCompleted(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onRemoteRejected(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onLocallyRejected(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onRemoteAccepted(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onLocallyAccepted(Certificate partnerCertificate) {
//
//            }
//        };
//        RWLock lock = new RWLock();
//        lock.lockWrite();
//        //todo continue gui
////        MelBoot spawn = new MelBoot(json1, DriveFXBootloader.class)
////                .addMelAuthAdmin(new MelAuthFxLoader());
////        spawn.spawn().done(result -> {
////            result.addRegisterHandler(new RegisterHandlerFX());
////            runner.r(() -> {
////                Lok.debug("DriveFXTest.startEmptyClient.booted");
////            });
////        });
////        lock.lockWrite();
////        lock.unlockWrite();
//    }
//
//    @Test
//    public void startEmptyServer() throws Exception {
//        File testdir = new File("testdir1");
//        CertificateManager.deleteDirectory(testdir);
//        CertificateManager.deleteDirectory(MelBoot.defaultWorkingDir1);
//        N runner = new N(e -> e.printStackTrace());
//        MelStandAloneAuthFX standAloneAuth1;
//        MelAuthSettings json1 = new MelAuthSettings().setPort(8888).setDeliveryPort(8889)
//                .setBrotcastListenerPort(9966).setBrotcastPort(6699)
//                .setWorkingDirectory(MelBoot.defaultWorkingDir1).setName("Test Server").setGreeting("greeting1");
//        IRegisterHandler allowRegisterHandler = new IRegisterHandler() {
//            @Override
//            public void acceptCertificate(IRegisterHandlerListener listener, MelRequest request, Certificate myCertificate, Certificate certificate) {
//                listener.onCertificateAccepted(request, certificate);
//            }
//
//            @Override
//            public void onRegistrationCompleted(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onRemoteRejected(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onLocallyRejected(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onRemoteAccepted(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onLocallyAccepted(Certificate partnerCertificate) {
//
//            }
//        };
//        RWLock lock = new RWLock();
//        lock.lockWrite();
//        MelBoot spawn = new MelBoot(json1, DriveFXBootloader.class);
//        spawn.addMelAuthAdmin(new MelAuthFxLoader());
//        spawn.spawn().done(melAuthService -> {
//            melAuthService.addRegisterHandler(new RegisterHandlerFX());
//            runner.r(() -> {
//                Lok.debug("DriveFXTest.startEmptyServer.booted");
//            });
//            N.r(() -> {
//                DriveCreateController createController = new DriveCreateController(melAuthService);
//                createController.createDriveServerService("testiServer", testdir.getAbsolutePath(),.1f,30);
//            });
//        });
//        lock.lockWrite();
//        lock.unlockWrite();
//    }
//
//    private void connectAcceptingClient() throws Exception {
//        File testdir = new File("testdir2");
//        CertificateManager.deleteDirectory(testdir);
//        CertificateManager.deleteDirectory(MelBoot.defaultWorkingDir2);
//        TestDirCreator.createTestDir(testdir);
//        N runner = new N(e -> e.printStackTrace());
//        MelAuthSettings melAuthSettings = new MelAuthSettings().setPort(8890).setDeliveryPort(8891)
//                .setBrotcastListenerPort(6699).setBrotcastPort(9966)
//                .setWorkingDirectory(MelBoot.defaultWorkingDir2).setName("Test Client").setGreeting("greeting2");
//        IRegisterHandler allowRegisterHandler = new IRegisterHandler() {
//            @Override
//            public void acceptCertificate(IRegisterHandlerListener listener, MelRequest request, Certificate myCertificate, Certificate certificate) {
//                listener.onCertificateAccepted(request, certificate);
//            }
//
//            @Override
//            public void onRegistrationCompleted(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onRemoteRejected(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onLocallyRejected(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onRemoteAccepted(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onLocallyAccepted(Certificate partnerCertificate) {
//
//            }
//        };
//        IRegisteredHandler allowRegisteredHandler = (melAuthService, registered) -> {
//            DatabaseManager databaseManager = melAuthService.getDatabaseManager();
//            for (ServiceJoinServiceType service : databaseManager.getAllServices()) {
//                databaseManager.grant(service.getServiceId().v(), registered.getId().v());
//            }
//        };
//        RWLock lock = new RWLock();
//        lock.lockWrite();
//        MelBoot spawn = new MelBoot(melAuthSettings, DriveFXBootloader.class);
//        spawn.addMelAuthAdmin(new MelAuthFxLoader());
//        spawn.spawn().done(melAuthService -> {
//            melAuthService.addRegisterHandler(allowRegisterHandler);
//            melAuthService.addRegisteredHandler(allowRegisteredHandler);
////            melAuthService.addRegisterHandler(new RegisterHandlerFX());
//            runner.r(() -> {
//                Lok.debug("DriveFXTest.startEmptyServer.booted");
//            });
//            N.r(() -> {
//                Promise<MelValidationProcess, Exception, Void> connected = melAuthService.connect("127.0.0.1", 8888, 8889, true);
//                connected.done(result -> N.r(() -> {
//                    DriveCreateController createController = new DriveCreateController(melAuthService);
//                    Promise<MelDriveClientService, Exception, Void> clientBooted = createController.createDriveClientService("drive client", testdir.getAbsolutePath(), 1L, tmp,.1f,30);
//                    Lok.debug("DriveFXTest.connectAcceptingClient");
//                    clientBooted.done(result1 -> Lok.debug("DriveFXTest.connectAcceptingClient.j89veaj4"));
//                }));
//
//            });
//        });
//    }
//
//    public static String tmp;
//
//    @Test
//    public void connectAccepting() throws Exception {
//        MelLogger.redirectSysOut(100, true);
//        File testdir = new File("testdir1");
//        CertificateManager.deleteDirectory(testdir);
//        CertificateManager.deleteDirectory(MelBoot.defaultWorkingDir1);
//        TestDirCreator.createTestDir(testdir);
//        N runner = new N(e -> e.printStackTrace());
//        MelAuthSettings melAuthSettings = new MelAuthSettings().setPort(8888).setDeliveryPort(8889)
//                .setBrotcastListenerPort(9966).setBrotcastPort(6699)
//                .setWorkingDirectory(MelBoot.defaultWorkingDir1).setName("Test Server").setGreeting("greeting1");
//        IRegisterHandler allowRegisterHandler = new IRegisterHandler() {
//            @Override
//            public void acceptCertificate(IRegisterHandlerListener listener, MelRequest request, Certificate myCertificate, Certificate certificate) {
//                listener.onCertificateAccepted(request, certificate);
//            }
//
//            @Override
//            public void onRegistrationCompleted(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onRemoteRejected(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onLocallyRejected(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onRemoteAccepted(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onLocallyAccepted(Certificate partnerCertificate) {
//
//            }
//        };
//        IRegisteredHandler allowRegisteredHandler = (melAuthService, registered) -> {
//            DatabaseManager databaseManager = melAuthService.getDatabaseManager();
//            for (ServiceJoinServiceType service : databaseManager.getAllServices()) {
//                databaseManager.grant(service.getServiceId().v(), registered.getId().v());
//            }
//        };
//        RWLock lock = new RWLock();
//        lock.lockWrite();
//        MelBoot spawn = new MelBoot(melAuthSettings, DriveFXBootloader.class);
//        spawn.addMelAuthAdmin(new MelAuthFxLoader());
//        spawn.spawn().done(melAuthService -> {
//            melAuthService.addRegisterHandler(allowRegisterHandler);
//            melAuthService.addRegisteredHandler(allowRegisteredHandler);
////            melAuthService.addRegisterHandler(new RegisterHandlerFX());
//            runner.r(() -> {
//                Lok.debug("DriveFXTest.startEmptyServer.booted");
//            });
//            N.r(() -> {
//                DriveCreateController createController = new DriveCreateController(melAuthService);
//                MelDriveServerService serverService = createController.createDriveServerService("testiServer", testdir.getAbsolutePath(),.1f,30);
//                DriveFXTest.tmp = serverService.getUuid();
//                connectAcceptingClient();
//            });
//        });
//        lock.lockWrite();
//        lock.unlockWrite();
//    }
//
//    ;
//
//    @Test
//    public void startAcceptingServer() throws Exception {
//        File testdir = new File("testdir1");
//        CertificateManager.deleteDirectory(testdir);
//        CertificateManager.deleteDirectory(MelBoot.defaultWorkingDir1);
//        TestDirCreator.createTestDir(testdir);
//        N runner = new N(e -> e.printStackTrace());
//        MelAuthSettings melAuthSettings = new MelAuthSettings().setPort(8888).setDeliveryPort(8889)
//                .setBrotcastListenerPort(9966).setBrotcastPort(6699)
//                .setWorkingDirectory(MelBoot.defaultWorkingDir1).setName("Test Server").setGreeting("greeting1");
//        IRegisterHandler allowRegisterHandler = new IRegisterHandler() {
//            @Override
//            public void acceptCertificate(IRegisterHandlerListener listener, MelRequest request, Certificate myCertificate, Certificate certificate) {
//                listener.onCertificateAccepted(request, certificate);
//            }
//
//            @Override
//            public void onRegistrationCompleted(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onRemoteRejected(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onLocallyRejected(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onRemoteAccepted(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onLocallyAccepted(Certificate partnerCertificate) {
//
//            }
//        };
//        IRegisteredHandler allowRegisteredHandler = (melAuthService, registered) -> {
//            DatabaseManager databaseManager = melAuthService.getDatabaseManager();
//            for (ServiceJoinServiceType service : databaseManager.getAllServices()) {
//                databaseManager.grant(service.getServiceId().v(), registered.getId().v());
//            }
//        };
//        RWLock lock = new RWLock();
//        lock.lockWrite();
//        MelBoot spawn = new MelBoot(melAuthSettings, DriveFXBootloader.class);
//        spawn.addMelAuthAdmin(new MelAuthFxLoader());
//        spawn.spawn().done(melAuthService -> {
//            melAuthService.addRegisterHandler(allowRegisterHandler);
//            melAuthService.addRegisteredHandler(allowRegisteredHandler);
////            melAuthService.addRegisterHandler(new RegisterHandlerFX());
//            runner.r(() -> {
//                Lok.debug("DriveFXTest.startEmptyServer.booted");
//            });
//            N.r(() -> {
//                DriveCreateController createController = new DriveCreateController(melAuthService);
//                createController.createDriveServerService("testiServer", testdir.getAbsolutePath(),.1f,30);
//            });
//        });
//        lock.lockWrite();
//        lock.unlockWrite();
//    }
//
//
//    @Test
//    public void startBoth() throws Exception, SqlQueriesException {
////        inject(true);
//        CertificateManager.deleteDirectory(MelBoot.defaultWorkingDir1);
//        CertificateManager.deleteDirectory(MelBoot.defaultWorkingDir2);
//        N runner = new N(e -> e.printStackTrace());
//        MelAuthSettings json1 = new MelAuthSettings().setPort(8888).setDeliveryPort(8889)
//                .setBrotcastListenerPort(9966).setBrotcastPort(6699)
//                .setWorkingDirectory(MelBoot.defaultWorkingDir1).setName("MA1").setGreeting("greeting1");
//        MelAuthSettings json2 = new MelAuthSettings().setPort(8890).setDeliveryPort(8891)
//                .setBrotcastPort(9966) // does not listen! only one listener seems possible
//                .setBrotcastListenerPort(6699).setBrotcastPort(9966)
//                .setWorkingDirectory(MelBoot.defaultWorkingDir2).setName("MA2").setGreeting("greeting2");
//
//        IRegisterHandler allowRegisterHandler = new IRegisterHandler() {
//            @Override
//            public void acceptCertificate(IRegisterHandlerListener listener, MelRequest request, Certificate myCertificate, Certificate certificate) {
//                listener.onCertificateAccepted(request, certificate);
//
//            }
//
//            @Override
//            public void onRegistrationCompleted(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onRemoteRejected(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onLocallyRejected(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onRemoteAccepted(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onLocallyAccepted(Certificate partnerCertificate) {
//
//            }
//        };
//
//
//
///*
//        IDBCreatedListener admin = databaseManager -> {
//            ServiceType serviceType = databaseManager.createServiceType("test type", "test type desc");
//            databaseManager.createService(serviceType.getId().v(), "service uuid");
//        };*/
//        //standAloneAuth1.addRegisterHandler(new RegisterHandlerFX());
//        //standAloneAuth2.addRegisterHandler(new RegisterHandlerFX());
//        /*standAloneAuth1.addRegisteredHandler((melAuthService, registered) -> {
//            List<ServiceJoinServiceType> services = melAuthService.getDatabaseManager().getAllServices();
//            for (ServiceJoinServiceType serviceJoinServiceType : services) {
//                melAuthService.getDatabaseManager().grant(serviceJoinServiceType.getServiceId().v(), registered.getId().v());
//            }
//        });*/
//        lock.lockWrite();
//
//        MelBoot spawn = new MelBoot(json1, DriveFXBootloader.class);
//        MelBoot boot2 = new MelBoot(json2, DriveFXBootloader.class);
//        spawn.spawn().done(standAloneAuth1 -> {
//            standAloneAuth1.addRegisterHandler(new RegisterHandlerFX());
//            runner.r(() -> {
//                Lok.debug("DriveFXTest.driveGui.1.booted");
////                DriveBootloader.deVinjector = null;
//                boot2.spawn().done(standAloneAuth2 -> {
//                    Lok.debug("DriveFXTest.driveGui.2.booted");
//                    standAloneAuth2.addRegisterHandler(new RegisterHandlerFX());
//                    runner.r(() -> {
////                        Promise<MelValidationProcess, Exception, Void> connectPromise = standAloneAuth2.connect(null, "localhost", 8888, 8889, true);
////                        connectPromise.done(integer -> {
////                            runner.r(() -> {
////                                Lok.debug("DriveFXTest.driveGui.booted");
////                                //standAloneAuth2.getBrotCaster().discover(9966);
////                                //lock.unlockWrite();
////                            });
////                        });
//                    });
//                });
//            });
//        });
//        lock.lockWrite();
//        lock.lockWrite();
//        lock.unlockWrite();
//    }
//
//    @Test
//    public void driveGui() throws Exception, SqlQueriesException {
////        inject(true);
//        CertificateManager.deleteDirectory(MelBoot.defaultWorkingDir1);
//        CertificateManager.deleteDirectory(MelBoot.defaultWorkingDir2);
//        N runner = new N(e -> e.printStackTrace());
//        MelAuthSettings json1 = new MelAuthSettings().setPort(8888).setDeliveryPort(8889)
//                .setBrotcastListenerPort(9966).setBrotcastPort(6699)
//                .setWorkingDirectory(MelBoot.defaultWorkingDir1).setName("MA1").setGreeting("greeting1");
//        MelAuthSettings json2 = new MelAuthSettings().setPort(8890).setDeliveryPort(8891)
//                .setBrotcastPort(9966) // does not listen! only one listener seems possible
//                .setBrotcastListenerPort(6699).setBrotcastPort(9966)
//                .setWorkingDirectory(MelBoot.defaultWorkingDir2).setName("MA2").setGreeting("greeting2");
//        IRegisterHandler allowRegisterHandler = new IRegisterHandler() {
//            @Override
//            public void acceptCertificate(IRegisterHandlerListener listener, MelRequest request, Certificate myCertificate, Certificate certificate) {
//                listener.onCertificateAccepted(request, certificate);
//            }
//
//            @Override
//            public void onRegistrationCompleted(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onRemoteRejected(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onLocallyRejected(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onRemoteAccepted(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onLocallyAccepted(Certificate partnerCertificate) {
//
//            }
//        };
//
//
//
///*
//        IDBCreatedListener admin = databaseManager -> {
//            ServiceType serviceType = databaseManager.createServiceType("test type", "test type desc");
//            databaseManager.createService(serviceType.getId().v(), "service uuid");
//        };*/
//        //standAloneAuth1.addRegisterHandler(new RegisterHandlerFX());
//        //standAloneAuth2.addRegisterHandler(new RegisterHandlerFX());
//        /*standAloneAuth1.addRegisteredHandler((melAuthService, registered) -> {
//            List<ServiceJoinServiceType> services = melAuthService.getDatabaseManager().getAllServices();
//            for (ServiceJoinServiceType serviceJoinServiceType : services) {
//                melAuthService.getDatabaseManager().grant(serviceJoinServiceType.getServiceId().v(), registered.getId().v());
//            }
//        });*/
//        lock.lockWrite();
//
//        MelBoot spawn = new MelBoot(json1, DriveFXBootloader.class);
//        MelBoot boot2 = new MelBoot(json2, DriveFXBootloader.class);
//        spawn.spawn().done(standAloneAuth1 -> {
//            standAloneAuth1.addRegisterHandler(new RegisterHandlerFX());
//            runner.r(() -> {
//                Lok.debug("DriveFXTest.driveGui.1.booted");
////                DriveBootloader.deVinjector = null;
//                boot2.spawn().done(standAloneAuth2 -> {
//                    Lok.debug("DriveFXTest.driveGui.2.booted");
//                    standAloneAuth2.addRegisterHandler(new RegisterHandlerFX());
//                    runner.r(() -> {
//                        Promise<MelValidationProcess, Exception, Void> connectPromise = standAloneAuth2.connect( "localhost", 8888, 8889, true);
//                        connectPromise.done(integer -> {
//                            runner.r(() -> {
//                                Lok.debug("DriveFXTest.driveGui.booted");
//                                //standAloneAuth2.getBrotCaster().discover(9966);
//                                //lock.unlockWrite();
//                            });
//                        });
//                    });
//                });
//            });
//        });
//        lock.lockWrite();
//        lock.unlockWrite();
//    }
//
//
//    public void setup(DriveSyncListener clientSyncListener) throws Exception, SqlQueriesException {
//        //setup working directories & directories with test data
//        File testdir1 = new File("testdir1");
//        File testdir2 = new File("testdir2");
//        CertificateManager.deleteDirectory(MelBoot.defaultWorkingDir1);
//        CertificateManager.deleteDirectory(MelBoot.defaultWorkingDir2);
//        CertificateManager.deleteDirectory(testdir1);
//        CertificateManager.deleteDirectory(testdir2);
//        TestDirCreator.createTestDir(testdir1);
//        testdir2.mkdirs();
//
//        // configure MelAuth
//        N runner = new N(e -> e.printStackTrace());
//
//        MelAuthSettings json1 = new MelAuthSettings().setPort(8888).setDeliveryPort(8889)
//                .setBrotcastListenerPort(9966).setBrotcastPort(6699)
//                .setWorkingDirectory(MelBoot.defaultWorkingDir1).setName("MA1").setGreeting("greeting1");
//        MelAuthSettings json2 = new MelAuthSettings().setPort(8890).setDeliveryPort(8891)
//                .setBrotcastPort(9966) // does not listen! only one listener seems possible
//                .setBrotcastListenerPort(6699).setBrotcastPort(9966)
//                .setWorkingDirectory(MelBoot.defaultWorkingDir2).setName("MA2").setGreeting("greeting2");
//        // we want accept all registration attempts automatically
//        IRegisterHandler allowRegisterHandler = new IRegisterHandler() {
//            @Override
//            public void acceptCertificate(IRegisterHandlerListener listener, MelRequest request, Certificate myCertificate, Certificate certificate) {
//                listener.onCertificateAccepted(request, certificate);
//
//            }
//
//            @Override
//            public void onRegistrationCompleted(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onRemoteRejected(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onLocallyRejected(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onRemoteAccepted(Certificate partnerCertificate) {
//
//            }
//
//            @Override
//            public void onLocallyAccepted(Certificate partnerCertificate) {
//
//            }
//        };
//        // we want to allow every registered Certificate to talk to all available Services
//        IRegisteredHandler registeredHandler = (melAuthService, registered) -> {
//            List<ServiceJoinServiceType> services = melAuthService.getDatabaseManager().getAllServices();
//            for (ServiceJoinServiceType serviceJoinServiceType : services) {
//                melAuthService.getDatabaseManager().grant(serviceJoinServiceType.getServiceId().v(), registered.getId().v());
//            }
//        };
//        lock.lockWrite();
//
//        MelBoot spawn = new MelBoot(json1, DriveFXBootloader.class);
//        MelBoot boot2 = new MelBoot(json2, DriveFXBootloader.class);
//        spawn.spawn().done(standAloneAuth1 -> {
//            runner.r(() -> {
//                Lok.debug("DriveFXTest.driveGui.1.booted");
//                standAloneAuth1.addRegisteredHandler(registeredHandler);
//                // setup the server Service
//                MelDriveServerService serverService = new DriveCreateController(standAloneAuth1).createDriveServerService("server service", testdir1.getAbsolutePath(),.1f,30);
//                boot2.spawn().done(standAloneAuth2 -> {
//                    Lok.debug("DriveFXTest.driveGui.2.booted");
//                    standAloneAuth2.addRegisterHandler(allowRegisterHandler);
//
//                    runner.r(() -> {
//                        // connect first. this step will register
//                        Promise<MelValidationProcess, Exception, Void> connectPromise = standAloneAuth2.connect( "localhost", 8888, 8889, true);
//                        connectPromise.done(melValidationProcess -> {
//                            runner.r(() -> {
//                                Lok.debug("DriveFXTest.driveGui.connected");
//                                // MAs know each other at this point. setup the client Service. it wants some data from the steps before
//                                Promise<MelDriveClientService, Exception, Void> promise = new DriveCreateController(standAloneAuth2).createDriveClientService("client service", testdir2.getAbsolutePath(), 1l, serverService.getUuid(),.1f,30);
//                                promise.done(clientDriveService -> runner.r(() -> {
//                                            Lok.debug("DriveFXTest attempting first syncFromServer");
//                                            clientSyncListener.testStructure.setMaClient(standAloneAuth2)
//                                                    .setMaServer(standAloneAuth1)
//                                                    .setClientDriveService(clientDriveService)
//                                                    .setServerDriveService(serverService)
//                                                    .setTestdir1(testdir1)
//                                                    .setTestdir2(testdir2);
//                                            clientDriveService.setSyncListener(clientSyncListener);
//                                            clientDriveService.syncThisClient();
//                                        }
//                                ));
//                            });
//                        });
//                    });
//                });
//            });
//        });
//        lock.lockWrite();
//        lock.unlockWrite();
//    }
//
//
//    @Test
//    public void firstSync() throws Exception {
//        DriveTest driveTest = new DriveTest();
//        MelBoot melBoot = new MelBoot(new DriveTest().createJson2(), DriveFXBootloader.class).addMelAuthAdmin(new MelAuthFxLoader());
//        driveTest.simpleTransferFromServerToClient(melBoot);
//        new WaitLock().lock().lock();
//    }
//
//
//    @Test
//    public void addFile() throws Exception {
//        setup(new DriveSyncListener() {
//            private int count = 0;
//
//            @Override
//            public void onSyncFailed() {
//
//            }
//
//            @Override
//            public void onTransfersDone() {
//
//            }
//
//            @Override
//            public void onSyncDoneImpl() {
//                try {
//                    if (count == 0) {
//                        DriveDatabaseManager dbManager = testStructure.clientDriveService.getDriveDatabaseManager();
//                        List<FsFile> rootFiles = dbManager.getFsDao().getFilesByFsDirectory(null);
//                        for (FsFile f : rootFiles) {
//                            Lok.debug(f.getName().v());
//                        }
//                        File newFile = new File(testStructure.testdir1.getAbsolutePath() + "/sub1/sub2.txt");
//                        newFile.createNewFile();
//                    } else if (count == 1) {
//                        Lok.debug("DriveFXTest.onSyncDoneImpl :)");
//                        Map<Long, GenericFSEntry> entries1 = genList2Map(testStructure.serverDriveService.getDriveDatabaseManager().getFsDao().getDelta(0));
//                        Map<Long, GenericFSEntry> entries2 = genList2Map(testStructure.clientDriveService.getDriveDatabaseManager().getFsDao().getDelta(0));
//                        Map<Long, GenericFSEntry> cp1 = new HashMap<>(entries1);
//                        cp1.forEach((id, entry) -> {
//                            if (entries2.containsKey(id)) {
//                                entries1.remove(id);
//                                entries2.remove(id);
//                            }
//                        });
//                        assertEquals(0, entries1.size());
//                        assertEquals(0, entries2.size());
//                        lock.unlockWrite();
//                    }
//                    count++;
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//    }
//
//    public Map<Long, GenericFSEntry> genList2Map(List<GenericFSEntry> entries) {
//        Map<Long, GenericFSEntry> map = new HashMap<>();
//        for (GenericFSEntry entry : entries) {
//            map.put(entry.getId().v(), entry);
//        }
//        return map;
//    }
//
//    @After
//    public void clean() {
//        standAloneAuth1 = standAloneAuth2 = null;
//        lock = null;
//    }
//
//    @Before
//    public void before() {
//        lock = new RWLock();
//    }


}