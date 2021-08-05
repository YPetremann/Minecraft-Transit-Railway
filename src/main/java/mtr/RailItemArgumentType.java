package mtr;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.tag.ItemTags;

public class RailItemArgumentType extends ItemStackArgumentType {

  public static RailItemArgumentType itemStack() {
    return new RailItemArgumentType();
  }

  public static <S> ItemStackArgument getItemStackArgument(CommandContext<S> context, String name) {
    return (ItemStackArgument)context.getArgument(name, ItemStackArgument.class);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    final String start        = builder.getRemaining().startsWith("mtr:rail_") ?
        builder.getInput() :
        builder.getInput().substring(0, builder.getStart()) + "mtr:rail_";
    StringReader stringReader = new StringReader(start);
    stringReader.setCursor(builder.getStart());
    ItemStringReader itemStringReader = new ItemStringReader(stringReader, false);

    SuggestionsBuilder newBuilder = new SuggestionsBuilder(start, builder.getStart());
    try {
      itemStringReader.consume();
    } catch (CommandSyntaxException var6) {
    }

    return itemStringReader.getSuggestions(newBuilder, ItemTags.getTagGroup());
  }
}
