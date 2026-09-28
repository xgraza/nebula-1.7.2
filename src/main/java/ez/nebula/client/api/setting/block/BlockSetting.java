package ez.nebula.client.api.setting.block;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author xgraza
 * @since 6/2/26
 */
public final class BlockSetting extends Setting<BlockValue>
{
    public BlockSetting(String name, String description, Predicate<BlockValue> visibility, Consumer<BlockValue> valueChanged, BlockValue value)
    {
        super(name, description, visibility, valueChanged, value);
    }

    public void setBlock(final Block block)
    {
        setValue(new BlockValue(block, getSubType()));
    }

    public void setSubType(final int subType)
    {
        setValue(new BlockValue(getBlock(), subType));
    }

    public Block getBlock()
    {
        return getValue().getBlock();
    }

    public int getSubType()
    {
        return getValue().getSubType();
    }

    public boolean isBlock(final ItemStack itemStack)
    {
        if (itemStack == null || !(itemStack.getItem() instanceof ItemBlock))
        {
            return false;
        }
        return ((ItemBlock) itemStack.getItem()).field_150939_a == getBlock()
                && itemStack.getItemDamage() == getSubType();
    }

    @Override
    public JsonElement toJSON()
    {
        final JsonObject object = new JsonObject();
        object.addProperty("id", Block.getIdFromBlock(getBlock()));
        object.addProperty("type", getSubType());
        return super.toJSON();
    }

    @Override
    public void fromJSON(JsonElement element)
    {
        if (!element.isJsonObject())
        {
            return;
        }
        final JsonObject object = element.getAsJsonObject();

        int subType = 0;
        if (object.has("type"))
        {
            subType = object.get("type").getAsInt();
        }

        Block block = null;
        if (object.has("id"))
        {
            final int id = object.get("id").getAsInt();
            block = Block.getBlockById(id);
            if (block == null)
            {
                throw new RuntimeException("invalid block ID " + id + "!");
            }
        }
        if (block == null)
        {
            block = getDefaultValue().getBlock();
        }

        setValue(new BlockValue(block, subType));
    }

    public static final class Builder extends Setting.Builder<BlockValue>
    {
        private Block block;
        private int subType;

        public Builder(String name)
        {
            super(name, null);
        }

        public Builder setBlock(Block block)
        {
            this.block = block;
            return this;
        }

        public Builder setSubType(int subType)
        {
            if (!BlockUtil.blockHasSubType(block))
            {
                return this;
            }
            this.subType = subType;
            return this;
        }

        @Override
        public BlockSetting build()
        {
            return new BlockSetting(name, description, visibility, valueChanged, new BlockValue(block, subType));
        }
    }
}
