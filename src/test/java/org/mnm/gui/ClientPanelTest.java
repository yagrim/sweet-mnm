package org.mnm.gui;

class ClientPanelTest {

    //    @Test
//    void shouldInvokeRepairActionWithMnmSlugWhenRepairIsClicked() {
//        AtomicReference<String> repairedSlug = new AtomicReference<>();
//        Client client = testClient();
//
//        var panel = GuiCommand.createClientPanel(null, client, false,
//            slug -> {
//                repairedSlug.set(slug);
//                return testClient();
//            }, (_, _) -> null,
//            _ -> {
//            });
//
//        findButton(panel, "Repair").doClick();
//
//        assertThat(repairedSlug.get()).isEqualTo("mnm");
//    }

//    @Test
//    void shouldDisableRepairButtonWhileRepairActionRuns() throws Exception {
//        CountDownLatch started = new CountDownLatch(1);
//        CountDownLatch release = new CountDownLatch(1);
//        Client client = testClient();
//
//        var panel = GuiCommand.createClientPanel(null, client, false,
//            slug -> {
//                started.countDown();
//                try {
//                    release.await(1, TimeUnit.SECONDS);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    throw new IllegalStateException(e);
//                }
//                return testClient();
//            }, (_, _) -> null, _ -> {
//            });
//        var repairButton = findButton(panel, "Repair");
//
//        repairButton.doClick();
//
//        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
//        assertThat(repairButton.isEnabled()).isFalse();
//
//        release.countDown();
//        waitForButtonEnabled(repairButton);
//
//        assertThat(repairButton.isEnabled()).isTrue();
//    }

//    @Test
//    void shouldInvokeRunActionWithMnmSlugWhenPlayIsClicked() {
//        AtomicReference<String> playedSlug = new AtomicReference<>();
//        Client client = testClient();
//
//        var panel = GuiCommand.createClientPanel(null, client, false,
//            _ -> null,
//            (_, _) -> null,
//            _ -> {
//            },
//            args -> playedSlug.set(args.get("slug")));
//
//        findButton(panel, "Play").doClick();
//
//        assertThat(playedSlug.get()).isEqualTo("mnm");
//    }
}
