package org.mnm.cli;

public class InstallCommand implements Command {

    @Override
    public void run() {
        System.out.println("Install Command");
    }

    @Override
    public String name() {
        return "install";
    }

    @Override
    public String help() {
        return "Installs MnM client in the current location.";
    }

}
