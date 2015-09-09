package gloomyfolken.hooklib.minecraft;

import cpw.mods.fml.relauncher.FMLRelaunchLog;
import gloomyfolken.hooklib.asm.AsmHook;
import gloomyfolken.hooklib.asm.HookClassTransformer;
import gloomyfolken.hooklib.asm.HookContainerParser;
import gloomyfolken.hooklib.asm.HookInjectorClassVisitor;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassWriter;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MinecraftClassTransformer extends HookClassTransformer implements IClassTransformer {

    static MinecraftClassTransformer instance;
    private HashMap<Integer, String> methodNames;

    public MinecraftClassTransformer() {
        instance = this;
        FMLRelaunchLog.makeLog("HookLib");
        logger = Logger.getLogger("HookLib");
        logger.fine("Loading secondary class transformer...");

        if (HookLibPlugin.getObfuscated()) {
            try {
                long timeStart = System.currentTimeMillis();
                methodNames = loadMethodNames();
                long time = System.currentTimeMillis() - timeStart;
                logger.log(Level.FINE, "Methods dictionary loaded in " + time + " ms");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Can not load obfuscated method names",  e);
            }
        }

        this.hooksMap.putAll(PrimaryClassTransformer.instance.getHooksMap());
        PrimaryClassTransformer.instance.getHooksMap().clear();
        PrimaryClassTransformer.instance.registeredSecondTransformer = true;
    }

    private HashMap<Integer, String> loadMethodNames() throws IOException {
        InputStream resourceStream = getClass().getResourceAsStream("/methods.bin");
        if (resourceStream == null) throw new IOException("Methods dictionary not found");
        DataInputStream input = new DataInputStream(new BufferedInputStream(resourceStream));
        int numMethods = input.readInt();
        HashMap<Integer, String> map = new HashMap<Integer, String>(numMethods);
        for (int i = 0; i < numMethods; i++) {
            map.put(input.readInt(), input.readUTF());
        }
        input.close();
        return map;
    }

    @Override
    public byte[] transform(String oldName, String newName, byte[] bytecode) {
        return transform(newName, bytecode);
    }

    @Override
    protected HookInjectorClassVisitor createInjectorClassVisitor(ClassWriter cw, List<AsmHook> hooks) {
        return new HookInjectorClassVisitor(cw, hooks) {
            @Override
            protected boolean isTargetMethod(AsmHook hook, String name, String desc) {
                if (name.startsWith("func_")) {
                    int first = name.indexOf('_');
                    int second = name.indexOf('_', first+1);
                    int methodId = Integer.valueOf(name.substring(first+1, second));
                    String mcpName = methodNames.get(methodId);
                    if (mcpName != null && super.isTargetMethod(hook, mcpName, desc)) {
                        return true;
                    }
                }
                return super.isTargetMethod(hook, name, desc);
            };
        };
    }
}
