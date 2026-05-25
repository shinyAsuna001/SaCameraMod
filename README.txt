SaCameraMod 使用说明
=================================
Release information:
- Author: shiny_Asuna
- License: MIT (please preserve original author credit; if modified and redistributed, note the original author shiny_Asuna)
- Cover image: PNG format, recommended 256x256, filename `logo.png`, placed in `src/main/resources/`, and set `logoFile="logo.png"` in `mods.toml`.

本节说明 `sacamera` 命令与模组提供的摄像机控制功能（中文说明）。

命令结构总览：
- 基础命令： `/sacamera <targets> <subcommand> ...`。
- `targets` 使用 Minecraft 的选择器或玩家名（例如 `@s`、`@a`、`playerName`）。

子命令与示例：
- `shake`：产生相机抖动。
   - 位置抖动：`/sacamera @s shake positioned <strength> <duration>`
   - 角度抖动：`/sacamera @s shake rotationed <strength> <duration>`
   - `strength` 为抖动强度，`duration` 为持续的 tick 数。

- `move`：平移摄像机。
   - 世界坐标：`/sacamera @s move <dx> <dy> <dz> <fadeIn> <hold> <fadeOut>`
   - 相对 yaw 方向：`/sacamera @s move relative <dx> <dy> <dz> <fadeIn> <hold> <fadeOut>`
   - 相对视向：`/sacamera @s move forward <dx> <dy> <dz> <fadeIn> <hold> <fadeOut>`
   - `forward` 会考虑当前视线 pitch，产生更自然的前进效果。

- `rotate`：旋转视角。
   - 相对旋转：`/sacamera @s rotate relative <axis> <angle> <fadeIn> <hold> <fadeOut>`
   - 绝对旋转：`/sacamera @s rotate <axis> <angle> <fadeIn> <hold> <fadeOut>`
   - `axis` 可为 `x` / `y` / `z`，其中 `z` 代表横滚（roll）。

- `cinema`：按关键帧插值播放运镜。
   - 语法：`/sacamera @s cinema <duration> <path>`。
   - `<duration>`：总 tick 时长。
   - `<path>`：由分号 `;` 分隔的关键帧序列。
   - 关键帧格式示例：
      - `x,y,z,yaw,pitch[,ease]`
      - `world:x,y,z,lookX,lookY,lookZ[,ease]`
   - `world:` 语法将相机置于绝对世界坐标并面对指定目标点。
   - `ease` 可选：`linear`（默认）、`easein`、`easeout`、`easeinout`。

- `toggleperspective`：强制切换视角类型。
   - 语法：`/sacamera @s toggleperspective <mode>`
   - `mode` 为 `0` / `1` / `2`，分别对应第一人称、第三人称背后、第三人称正面。

- `cancel`：取消当前所有摄像机效果并还原视角。
   - 使用：`/sacamera @s cancel`

常见命令示例：
- `/sacamera @s shake positioned 0.5 20`
- `/sacamera @s move forward 0 0 1 0 40 10`
- `/sacamera @s rotate relative y 45 10 40 10`
- `/sacamera @s cinema 100 1,4,1,45,-10;5,5,5,50,-5`
- `/sacamera @s cinema 100 world:1,5,1,0,4,0,easein;world:10,8,10,0,4,0,easeinout`
- `/sacamera @s toggleperspective 1`
- `/sacamera @s cancel`

网络行为与注意事项：
- 运行命令必须拥有权限
- 命令在服务器端执行后会向每个目标客户端发送独立控制包。
- 在单人集成服务器中，仍然通过服务端发送包到目标客户端。
- 本mod不包含渲染未加载区块，如果摄像机进入未加载区块并不会主动渲染和加载该区块

开发者说明：
- `move forward` 使用了视向前向和视向上向的向量计算，保证移动更自然地跟随玩家当前视角。
- `cinema` 支持以第一个关键帧作为起始点立即生效，后续关键帧按总时长插值更新位置与朝向。
- `toggleperspective` 通过客户端反射写入 `Minecraft.options.cameraType`（兼容 enum 和 int 字段表示）。
- 本mod由AI辅助开发

