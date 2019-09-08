package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.asm.HookClassTransformer;
import gloomyfolken.hooklib.asm.model.method.hook.AsmHook;
import net.minecraft.launchwrapper.IClassTransformer;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Этот трансформер занимается вставкой хуков с момента запуска майнкрафта. Здесь сосредоточены все костыли,
 * которые необходимы для правильной работы с обфусцированными названиями методов.
 */
public class MinecraftClassTransformer extends HookClassTransformer implements IClassTransformer {

    public static MinecraftClassTransformer instance;
    private Map<Integer, String> methodNames;

    private static List<IClassTransformer> postTransformers = new ArrayList<>();

    public MinecraftClassTransformer() {
        instance = this;

        if (HookLibPlugin.isObfuscated()) {
            try {
                long timeStart = System.currentTimeMillis();
                methodNames = loadMethodNames();
                long time = System.currentTimeMillis() - timeStart;
                logger.debug("Methods dictionary loaded in " + time + " ms");
            } catch (IOException e) {
                logger.severe("Can not load obfuscated method names", e);
            }
        }

        classMetadataReader = HookLoader.getDeobfuscationMetadataReader();

        hookMap.putAll(PrimaryClassTransformer.instance.getHooksMap());
        PrimaryClassTransformer.instance.getHooksMap().clear();
        PrimaryClassTransformer.instance.registeredSecondTransformer = true;
    }

    private HashMap<Integer, String> loadMethodNames() throws IOException {
        InputStream resourceStream = getClass().getResourceAsStream("/methods.bin");
        if (resourceStream == null) throw new IOException("Methods dictionary not found");
        DataInputStream input = new DataInputStream(new BufferedInputStream(resourceStream));
        int numMethods = input.readInt();
        HashMap<Integer, String> map = new HashMap<>(numMethods);
        for (int i = 0; i < numMethods; i++) {
            map.put(input.readInt(), input.readUTF());
        }
        input.close();
        return map;
    }

    @Override
    public byte[] transform(String oldName, String newName, byte[] bytecode) {
        bytecode = transform(newName, bytecode);
        for (int i = 0; i < postTransformers.size(); i++) {
            bytecode = postTransformers.get(i).transform(oldName, newName, bytecode);
        }
        return bytecode;
    }

    @Override
    public boolean isTargetMethod(AsmHook ah, String name, String desc) {
        return super.isTargetMethod(ah, deobfNameOfMethod(name), desc);
    }

    private String deobfNameOfMethod(String name) {
        return HookLibPlugin.isObfuscated()
                ? MinecraftClassTransformer.instance.getMethodNames().getOrDefault(MinecraftClassTransformer.getMethodId(name), name)
                : name;
    }

    public boolean areMethodNamesEquals(String name1, String name2) {
        return deobfNameOfMethod(name1).equals(deobfNameOfMethod(name2));
    }

    public Map<Integer, String> getMethodNames() {
        return methodNames;
    }

    public static int getMethodId(String srgName) {
        if (srgName.startsWith("func_")) {
            int first = srgName.indexOf('_');
            int second = srgName.indexOf('_', first + 1);
            return Integer.valueOf(srgName.substring(first + 1, second));
        } else {
            return -1;
        }
    }

    /**
     * Регистрирует трансформер, который будет запущен после обычных, и в том числе после деобфусцирующего трансформера.
     */
    public static void registerPostTransformer(IClassTransformer transformer) {
        postTransformers.add(transformer);
    }
}
