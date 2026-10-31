# XTCSysAgent 重写规范（所有子任务必读）

## 目标
把 \`D:\\CaremiumWorkspace\\Research\\SystemApps\\XTCSysAgent\\decompiled\\sources\\com\\xtc\\...\`
下的反编译 Java **纯手写重写**到
\`D:\\CaremiumWorkspace\\Sources\\XTCSysAgent\\app\\src\\main\\java\\...\`。

最终工程必须能 \`gradlew :app:assembleDebug\` 成功，行为与原厂 APK 完全一致。

## 铁律（违反即返工）
1. **禁止直接复制反编译源码再改**。要用自己的方式重新表达：补全泛型、
   把 \`Object\` 强转改成正确类型、删除所有 JADX 痕迹、写成正常 Java。
2. **禁止保留任何混淆痕迹**：
   - 不允许出现 \`/* JADX INFO ... */\` 注释；
   - 不允许出现 \`arg$1\` / \`this$0\` / \`val$xxx\` / \`lambda$xxx$0\` 这类反编译合成物；
   - 反编译出来的 \`$1\` \`$2\` 匿名内部类、\`$$Lambda$0\` 要还原成 **lambda 表达式**
     或 **有名字的静态/内部类**（见下面"匿名类还原"）；
   - 形如 \`a\` \`b\` \`c\` \`f\` 这类单字母的类名/方法名/字段名，**必须推断出有意义的名字**
     （见"去混淆命名"）。
3. **逻辑一个字都不许改**：常量值、字符串字面量、判断条件、调用顺序、
   线程模型、异常吞掉的位置、日志内容和 tag，全部保持原样。
   连 \`e.printStackTrace()\` 这类"看着不优雅"的写法也要保留，因为行为一致优先。
4. **必须能编译**。用 import 而不是全限定名（除非两个类同名冲突）。
5. 每个类、每个非平凡方法都要有中文 Javadoc / 行内注释，解释"为什么"，
   而不是复述代码。

## 语言
原厂全是 Java（APK 里没有任何 Kotlin 元数据），所以**一律写 Java**，
不要写 Kotlin。

## 去混淆命名
反编译后的包名 \`com.xtc.utils.encode\` 下 12 个单字母类，需要按语义改名。
改名规则：
- 类名改成 UpperCamelCase 的语义名；
- 方法/字段改成 lowerCamelCase 的语义名；
- 保持 \`public\` 可见性、\`static\`、返回类型、参数个数和类型不变。

**每改名一次，必须登记到 \`docs/NAMING.md\`**（格式：\`旧全限定名 -> 新全限定名：依据\`）。

如果实在推断不出语义（例如纯常量容器），就用中性但可读的名字，
并在注释里说明"原类名已混淆，按用途推断"。

## 匿名类还原
反编译里常见的形态：
\`\`\`java
private View.OnClickListener myUploadListener = new View.OnClickListener(this) {
    private final LocalFetchSecretActivity arg$1;
    { this.arg$1 = this; }
    public void onClick(View v) { this.arg$1.lambda$new$1$LocalFetchSecretActivity(v); }
};
\`\`\`
应还原成：
\`\`\`java
private final View.OnClickListener myUploadListener = new View.OnClickListener() {
    @Override public void onClick(View v) { judgeSecretCode(); }
};
\`\`\`
或者还原成 lambda：\`v -> judgeSecretCode()\`。

反编译里的 \`lambda$new$1$XxxActivity\` 方法，把方法体**内联**到调用点的
lambda / 匿名类里，不要保留这个合成方法。

## R 类
- 应用自己的资源用 \`com.xtc.systemagent.R\`，**该类由 AGP 自动生成，不要手写**。
  它的 id 已通过 \`app/src/main/res/values/public.xml\` 钉死为原厂值。
- 各库包自带的 \`com.xtc.xxx.R\` 是独立的小 R 类（只含本库用到的少数 id），
  **需要手写重建**，id 数值保持原值（原值可从反编译源码里直接读到）。
- 资源 id 千万不要去 \`R\` 之外硬编码。

## 依赖与已知类
- support 库：25.4.0（\`android.support.*\`），**不是 AndroidX**。
- okhttp 3.12.0 / okio 1.15.0 / retrofit 2.9.0 / rxjava 1.3.8 / ormlite 5.1 /
  qiniu-android-sdk 8.5.2 / gson 2.8.9 / jackson 2.9.10 / mosby mvp 2.0.1。
- \`com.tencent.mars.xlog.IXlog\` 已经手写好了。
- \`com.xtc.log.*\` 已经手写好了（Log / LogUtil / LogConfig / ILogger /
  CrashHandler / CrashListener / StackUtils / TextUtils）。
- \`com.xtc.systemagent.util.SdCardUtil\` 已经手写好了。

## 校验
写完一批后，用
\`\`\`powershell
Set-Location D:\\CaremiumWorkspace\\Sources\\XTCSysAgent
.\\gradlew.bat :app:compileDebugJavaWithJavac --console=plain 2>&1 | Select-Object -Last 80
\`\`\`
自检。有报错就改到没有为止。**注意只改自己负责的文件**，不要动别人的。
