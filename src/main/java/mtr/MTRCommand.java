package mtr;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.RootCommandNode;
import mtr.item.ItemRailModifier;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.argument.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Pair;
import net.minecraft.world.World;

final class MTRCommand implements Command<ServerCommandSource> {
  public static void register() {
    BlockPosArgumentType blockPosAT = BlockPosArgumentType.blockPos();
    RailItemArgumentType itemStackAT = RailItemArgumentType.itemStack();
    BoolArgumentType boolAT = BoolArgumentType.bool();
    IntegerArgumentType pIntAT = IntegerArgumentType.integer(0);

    CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
      RootCommandNode<ServerCommandSource> root = dispatcher.getRoot();
      root.addChild(CommandManager.literal("mtr")
          .requires(source -> source.hasPermissionLevel(2))
          .then(CommandManager.literal("option")
              .then(CommandManager.literal("use_mtr_font")
                  .executes(MTRCommand::optionMtrFont)
                  .then(CommandManager.argument("value",boolAT)
                      .executes(MTRCommand::optionMtrFont)
                  )
              )
              .then(CommandManager.literal("show_announcement_messages")
                  .executes(MTRCommand::optionNextStationChat)
                  .then(CommandManager.argument("value",boolAT)
                      .executes(MTRCommand::optionNextStationChat)
                  )
              )
              .then(CommandManager.literal("use_tts_announcements")
                  .executes(MTRCommand::optionNextStationTts)
                  .then(CommandManager.argument("value",boolAT)
                      .executes(MTRCommand::optionNextStationTts)
                  )
              )
              .then(CommandManager.literal("use_dynamic_fps")
                  .executes(MTRCommand::optionBoostFps)
                  .then(CommandManager.argument("value",boolAT)
                      .executes(MTRCommand::optionBoostFps)
                  )
              )
          )
          .then(CommandManager.literal("rail")
              .then(CommandManager.literal("connect")
                  .then(CommandManager.argument("type", itemStackAT)
                      .then(CommandManager.argument("start", blockPosAT)
                          .then(CommandManager.argument("end", blockPosAT)
                              .executes(MTRCommand::railConnect)
                          )
                      )
                  )
              )
              .then(CommandManager.literal("disconnect")
                  .then(CommandManager.argument("start", blockPosAT)
                      .executes(MTRCommand::railDisconnect)
                      .then(CommandManager.argument("end", blockPosAT)
                          .executes(MTRCommand::railDisconnect)
                      )
                  )
              )
          )
          .then(CommandManager.literal("platform")
              .executes(MTRCommand::platformList)
              .then(CommandManager.literal("create")
                  .then(CommandManager.argument("platform number", pIntAT)
                      .then(CommandManager.argument("start", blockPosAT)
                          .then(CommandManager.argument("end", blockPosAT)
                              .executes(MTRCommand::railConnect)
                          )
                      )
                  )
              )
              .then(CommandManager.literal("<station:platform>")
                  .then(CommandManager.literal("number")
                      .executes(MTRCommand::platformNumber)
                      .then(CommandManager.literal("<number>")
                          .executes(MTRCommand::platformNumber)
                      )
                  )
                  .then(CommandManager.literal("wait")
                      .executes(MTRCommand::platformWait)
                      .then(CommandManager.literal("<number>")
                          .executes(MTRCommand::platformWait)
                      )
                  )
              )
          )
          .then(CommandManager.literal("siding")
              .executes(MTRCommand::sidingList)
              .then(CommandManager.literal("create")
                  .then(CommandManager.argument("siding number", pIntAT)
                      .then(CommandManager.argument("start", blockPosAT)
                          .then(CommandManager.argument("end", blockPosAT)
                              .executes(MTRCommand::railConnect)
                          )
                      )
                  )
              )
              .then(CommandManager.literal("<depot:siding>")
                  .then(CommandManager.literal("number")
                      .executes(MTRCommand::sidingNumber)
                      .then(CommandManager.literal("<number>")
                          .executes(MTRCommand::sidingNumber)
                      )
                  )
                  .then(CommandManager.literal("train")
                      .executes(MTRCommand::sidingTrain)
                      .then(CommandManager.literal("<number>")
                          .executes(MTRCommand::sidingTrain)
                      )
                  )
                  .then(CommandManager.literal("unlimited")
                      .executes(MTRCommand::sidingUnlimited)
                      .then(CommandManager.literal("<number>")
                          .executes(MTRCommand::sidingUnlimited)
                      )
                  )
              )
          )
          .then(CommandManager.literal("station")
              .executes(MTRCommand::stationList)
              .then(CommandManager.literal("create")
                  .then(CommandManager.literal("<name>")
                      .then(CommandManager.argument("start", blockPosAT)
                          .then(CommandManager.argument("end", blockPosAT)
                              .executes(MTRCommand::stationCreate)
                              .then(CommandManager.literal("<color>")
                                  .executes(MTRCommand::stationCreate)
                              )
                          )
                      )
                  )
              )
              .then(CommandManager.literal("<station>")
                  .then(CommandManager.literal("delete")
                      .executes(MTRCommand::stationDelete)
                  )
                  .then(CommandManager.literal("name")
                      .executes(MTRCommand::stationName)
                      .then(CommandManager.literal("<name>")
                          .executes(MTRCommand::stationName)
                      )
                  )
                  .then(CommandManager.literal("color")
                      .executes(MTRCommand::stationColor)
                      .then(CommandManager.literal("<color>")
                          .executes(MTRCommand::stationColor)
                      )
                  )
                  .then(CommandManager.literal("number")
                      .executes(MTRCommand::stationNumber)
                      .then(CommandManager.literal("<number>")
                          .executes(MTRCommand::stationNumber)
                      )
                  )
                  .then(CommandManager.literal("area")
                      .executes(MTRCommand::stationArea)
                      .then(CommandManager.argument("start", blockPosAT)
                          .then(CommandManager.argument("end", blockPosAT)
                              .executes(MTRCommand::stationArea)
                          )
                      )
                  )
              )
          )
          .then(CommandManager.literal("exit")
              .then(CommandManager.literal("<station>")
                  .executes(MTRCommand::stationExitList)
                  .then(CommandManager.literal("delete")
                      .then(CommandManager.literal("all")
                          .executes(MTRCommand::stationExitDelete)
                      )
                      .then(CommandManager.literal("<code>")
                          .executes(MTRCommand::stationExitDelete)
                          .then(CommandManager.literal("<destination>")
                              .executes(MTRCommand::stationExitDelete)
                          )
                      )
                  )
                  .then(CommandManager.literal("insert")
                      .then(CommandManager.literal("<code>")
                          .executes(MTRCommand::stationExitInsert)
                          .then(CommandManager.literal("<destination>")
                              .executes(MTRCommand::stationExitInsert)
                              .then(CommandManager.literal("<indice>")
                                  .executes(MTRCommand::stationExitInsert)
                              )
                          )
                      )
                  )
              )
          )
          .then(CommandManager.literal("route")
              .executes(MTRCommand::routeList)
              .then(CommandManager.literal("create")
                  .then(CommandManager.literal("<name>")
                      .executes(MTRCommand::depotList)
                  )
              )
              .then(CommandManager.literal("<route>")
                  .then(CommandManager.literal("delete")
                      .executes(MTRCommand::routeDelete)
                  )
                  .then(CommandManager.literal("station")
                      .then(CommandManager.literal("delete")
                          .then(CommandManager.literal("all")
                          )
                          .then(CommandManager.literal("<indice>")
                              .executes(MTRCommand::routeStationDelete)
                          )
                          .then(CommandManager.literal("<station:platform>")
                              .executes(MTRCommand::routeStationDelete)
                          )
                      )
                      .then(CommandManager.literal("insert")
                          .then(CommandManager.literal("<station:platform>")
                              .executes(MTRCommand::routeStationInsert)
                              .then(CommandManager.literal("<indice>")
                                  .executes(MTRCommand::routeStationInsert)
                              )
                          )
                      )
                  )
              )
          )
          .then(CommandManager.literal("depot")
              .executes(MTRCommand::depotList)
              .then(CommandManager.literal("create")
                  .then(CommandManager.literal("<name>")
                      .then(CommandManager.argument("start", blockPosAT)
                          .then(CommandManager.argument("end", blockPosAT)
                              .executes(MTRCommand::depotCreate)
                              .then(CommandManager.literal("<color>")
                                  .executes(MTRCommand::depotCreate)
                              )
                          )
                      )
                  )
              )
              .then(CommandManager.literal("<depot>")
                  .then(CommandManager.literal("delete")
                      .executes(MTRCommand::depotDelete)
                  )
                  .then(CommandManager.literal("name")
                      .executes(MTRCommand::depotName)
                      .then(CommandManager.literal("<name>")
                          .executes(MTRCommand::depotName)
                      )
                  )
                  .then(CommandManager.literal("color")
                      .executes(MTRCommand::depotColor)
                      .then(CommandManager.literal("<color>")
                          .executes(MTRCommand::depotColor)
                      )
                  )
                  .then(CommandManager.literal("time")
                      .executes(MTRCommand::depotTime)
                      .then(CommandManager.literal("<startTime>")
                          .then(CommandManager.literal("<endTime>")
                              .then(CommandManager.literal("<frequency>")
                                  .executes(MTRCommand::depotTime)
                              )
                          )
                      )
                  )
                  .then(CommandManager.literal("area")
                      .executes(MTRCommand::depotArea)
                      .then(CommandManager.argument("start", blockPosAT)
                          .then(CommandManager.argument("end", blockPosAT)
                              .executes(MTRCommand::depotArea)
                          )
                      )
                  )
              )
              .then(CommandManager.literal("path")
                  .then(CommandManager.literal("<depot>")
                      .executes(MTRCommand::depotPathList)
                      .then(CommandManager.literal("delete")
                          .then(CommandManager.literal("all")
                              .executes(MTRCommand::depotPathDelete)
                          )
                          .then(CommandManager.literal("<indice>")
                              .executes(MTRCommand::depotPathDelete)
                          )
                      )
                      .then(CommandManager.literal("insert")
                          .then(CommandManager.literal("<route>")
                              .executes(MTRCommand::depotPathInsert)
                              .then(CommandManager.literal("<indice>")
                                  .executes(MTRCommand::depotPathInsert)
                              )
                          )
                      )
                  )
                  .then(CommandManager.literal("refresh")
                      .executes(MTRCommand::depotRefresh)
                  )
              )
          )
          .build()
      );
    });

  }

  /**************************
   * Options Based Commands *
   **************************/
  public static int optionMtrFont(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }
  public static int optionNextStationChat(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }
  public static int optionNextStationTts(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }
  public static int optionBoostFps(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }
  /***********************
   * Rail Based Commands *
   ***********************/
  public static int railConnect(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    final String input = context.getInput();
    final String command = input.startsWith("/") ? input.substring(1):input;
    final Item type;
    if(command.startsWith("mtr platform")){
      final int platformNumber = IntegerArgumentType.getInteger(context, "platform number");
      // System.out.println(platformNumber);
      type = new ItemStack(Items.RAIL_CONNECTOR_PLATFORM).getItem();
    } else if(command.startsWith("mtr siding")){
      final int sidingNumber = IntegerArgumentType.getInteger(context, "siding number");
      // System.out.println(sidingNumber);
      type = new ItemStack(Items.RAIL_CONNECTOR_SIDING).getItem();
    } else if (command.startsWith("mtr rail")) {
      type = RailItemArgumentType.getItemStackArgument(context, "type").getItem();
      // System.out.println(type);
    } else {
      source.sendFeedback(Text.of("Unimplemented"), true);
      return 0;
    }


    if (!(type instanceof ItemRailModifier)) {
      source.sendFeedback(new LiteralText("Item " + type.getName() + " can't be used"), true);
      return -1;
    }
    final ItemRailModifier railModifier = (ItemRailModifier) type;
    final World world = source.getWorld();
    final BlockPos endPos = BlockPosArgumentType.getBlockPos(context, "start");
    final BlockPos startPos = BlockPosArgumentType.getBlockPos(context, "end");

    final Pair<ActionResult, Text> result = railModifier.placeAction(world, startPos, endPos);
    final Text message = result.getRight();
    if (message.asString().length() > 0) {
      source.sendFeedback(message, true);
    }
    switch (result.getLeft()) {
    case SUCCESS:
      return 1;
    case FAIL:
      return -1;
    default:
      return 0;
    }
  }

  public static int railDisconnect(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  /***************************
   * Platform Based Commands *
   ***************************/
  public static int platformList(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  public static int platformNumber(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  public static int platformWait(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  /*************************
   * Siding Based Commands *
   *************************/
  public static int sidingList(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  public static int sidingNumber(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  public static int sidingTrain(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  public static int sidingUnlimited(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  /**************************
   * Station Based Commands *
   **************************/
  public static int stationList(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  public static int stationCreate(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  public static int stationDelete(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  public static int stationName(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  public static int stationColor(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  public static int stationNumber(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  public static int stationArea(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  public static int stationExitList(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }
  public static int stationExitDelete(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }
  public static int stationExitInsert(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  /************************
   * Route Based Commands *
   ************************/

  public static int routeList(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  public static int routeDelete(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  public static int routeStationDelete(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  public static int routeStationInsert(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }
  
  /************************
   * Depot Based Commands *
   ************************/
  
  public static int depotList(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }

  public static int depotCreate(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }
  public static int depotDelete(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }
  public static int depotRefresh(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }
  public static int depotName(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }
  public static int depotColor(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }
  public static int depotTime(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }
  public static int depotArea(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }
  public static int depotPathList(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }
  public static int depotPathDelete(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }
  public static int depotPathInsert(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    source.sendFeedback(Text.of("Unimplemented"), true);
    return 0;
  }
  @Override
  public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return 0;
  }
}
