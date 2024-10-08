package gjum.minecraft.civ.synapse.mod;

import static gjum.minecraft.civ.synapse.common.Util.dateFmtHms;

import gjum.minecraft.civ.synapse.common.observations.AccountObservation;
import gjum.minecraft.civ.synapse.common.observations.Observation;
import java.util.ArrayList;
import java.util.Date;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

/**
 * syntax:
 * {var} - var, if present; otherwise empty string
 * {$var$ } - prefix/suffix content around var, if present
 * {$red$...} - set color for ... part
 * {{snitch}$hover$Cull: {cull}\nType: {type}} - show message on mouseover
 * {[{coords}]$suggest$at [{coords}]} - put `at [{coords}]` in chat
 */
public class FormatPattern {
    @NotNull
    private final CollectionNode root;

    @NotNull
    private final String fmt;

    public FormatPattern(String fmt) {
        this.fmt = fmt;
        root = new CollectionNode(new ArrayList<>());
        int index = 0;
        while (index < fmt.length()) {
            final CollectionNode node = parseUntilNextSepOrEnd(fmt, index);
            root.nodes.add(node);
            index = node.end;
            if (index < fmt.length()) { // stopped at stray $ or }
                root.nodes.add(new RawNode(fmt.substring(index, index + 1)));
                index += 1; // swallow $ or }
            }
        }
    }

    private static CollectionNode parseUntilNextSepOrEnd(String fmt, int index) {
        // WARNING very brittle. rewrite instead of fixing.
        // TODO rewrite parser: first tokenize, then convert to tree
        final ArrayList<PatternNode> nodes = new ArrayList<>();
        int prevIndex = index - 1;
        while (true) {
            if (index <= prevIndex) { // should never happen as we +1 everywhere, but this code is so brittle we might as well check this
                System.err.println("ERROR: Parser made no progress in prev iteration. index=" + index + " prev=" + prevIndex);
                return new CollectionNode(nodes, index);
            }
            prevIndex = index;

            final int templateStart = fmt.indexOf('{', index);
            final int leadSep = minus1IsOther(fmt.indexOf('$', index), fmt.length());
            final int leadEnd = minus1IsOther(fmt.indexOf('}', index), fmt.length());
            if (-1 == templateStart // no templates remaining
                    || leadSep < templateStart // no templates before next sep
                    || leadEnd < templateStart // no templates before next end
            ) {
                if (leadSep == -1 && leadEnd == -1) { // add leftover content from index to string end
                    nodes.add(new RawNode(fmt.substring(index)));
                    index = fmt.length();
                } else { // add leftover content from index to the lead $ or }
                    final int rem = Math.min(leadSep, leadEnd);
                    if (rem - index > 0) nodes.add(new RawNode(fmt.substring(index, rem)));
                    index = rem;
                }
                break;
            }

            if (templateStart - index > 0) nodes.add(new RawNode(fmt.substring(index, templateStart)));
            index = templateStart + 1;

            final int nextSep = fmt.indexOf('$', index);
            final int nextEnd = fmt.indexOf('}', index);
            final int nextStart = fmt.indexOf('{', index);
            if (nextEnd != -1 && (nextStart == -1 || nextEnd < nextStart) && (nextSep == -1 || nextEnd < nextSep)) {
                // {var}
                final String var = fmt.substring(index, nextEnd);
                if (var.length() <= 0) {
                    nodes.add(new RawNode("{}"));
                } else {
                    nodes.add(new TemplateNode(var));
                }
                index = nextEnd + 1;
            } else { // {pre$var$post}, where pre/post potentially have more {}
                final CollectionNode resultPre = parseUntilNextSepOrEnd(fmt, index);
                index = resultPre.end + 1; // swallow $ or }

                final int postSep = fmt.indexOf('$', index);
                final int postEnd = fmt.indexOf('}', index);
                if (-1 == postSep) { // no further $
                    if (postEnd == -1) { // no $ or }. add remainder
                        nodes.add(new TemplateNode(resultPre, "", new RawNode(fmt.substring(index))));
                        index = fmt.length();
                    } else { // there's a } but no $ add before }
                        nodes.add(new TemplateNode(resultPre, "", new RawNode(fmt.substring(index, postEnd))));
                        index = postEnd + 1; // swallow }
                    }
                    break;
                }
                if (postEnd != -1 && postEnd < postSep) { // } before $. add before }
                    nodes.add(new TemplateNode(resultPre, "", new RawNode(fmt.substring(index, postEnd))));
                    index = postEnd + 1; // swallow }
                    break;
                }

                final String var = fmt.substring(index, postSep);

                final CollectionNode resultPost = parseUntilNextSepOrEnd(fmt, postSep + 1);

                nodes.add(new TemplateNode(resultPre, var, resultPost));

                index = resultPost.end + 1; // swallow } (also $ but that is ok, it would mean we were passed something deformed like {a$b$c$d})
            }
        }
        return new CollectionNode(nodes, index);
    }

    private static int minus1IsOther(int num, int other) {
        return num != -1 ? num : other;
    }

    @NotNull
    public String getFmt() {
        return fmt;
    }

    @NotNull
    public MutableComponent format(Observation observation) {
        return root.format(observation);
    }

    private interface PatternNode {
        MutableComponent format(Observation observation);
    }

    private static class RawNode implements PatternNode {
        public static final RawNode EMPTY = new RawNode("");

        public final String s;

        private RawNode(String s) {
            this.s = s;
        }

        @Override
        public MutableComponent format(Observation observation) {
            return Component.literal(s);
        }
    }

    private static class CollectionNode implements PatternNode {
        private final ArrayList<PatternNode> nodes;

        /**
         * hack for parsing. do not use
         */
        @Deprecated
        private final int end;

        private CollectionNode(ArrayList<PatternNode> nodes) {
            this(nodes, -1);
        }

        private CollectionNode(ArrayList<PatternNode> nodes, int end) {
            this.nodes = nodes;
            this.end = end;
        }

        @Override
        public MutableComponent format(Observation observation) {
            final MutableComponent result = Component.empty();
            for (PatternNode node : nodes) {
                result.append(node.format(observation));
            }
            return result;
        }
    }

    private static class TemplateNode implements PatternNode {
        String mid = "";
        PatternNode pre = RawNode.EMPTY;
        PatternNode post = RawNode.EMPTY;

        private TemplateNode(String mid) {
            this.mid = mid.toLowerCase();
        }

        private TemplateNode(PatternNode pre, String mid, PatternNode post) {
            this.pre = pre;
            this.mid = mid;
            this.post = post;
        }

        @Override
        public MutableComponent format(Observation observation) {
            switch (mid) {
                case "hover": {
                    final MutableComponent preFmtd = pre.format(observation);
                    preFmtd.getStyle().withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            post.format(observation)
                    ));
                    return preFmtd;
                }
                case "suggest": {
                    final MutableComponent preFmtd = pre.format(observation);
                    preFmtd.getStyle().withClickEvent(new ClickEvent(
                            ClickEvent.Action.SUGGEST_COMMAND,
                            post.format(observation).getString()
                    ));
                    return preFmtd;
                }
                case "time": {
                    final String timeStr = dateFmtHms.format(new Date(observation.getTime()));
                    return pre.format(observation)
                            .append(Component.literal(timeStr))
                            .append(post.format(observation));
                }
                case "account":
                case "player":
                case "name": {
                    // XXX prisoner, holder -> show main account optionally
                    if (observation instanceof final AccountObservation accountObservation) {
                        // XXX color
                        return pre.format(observation)
                                .append(Component.literal(accountObservation.getAccount()))
                                .append(post.format(observation));
                    }
                }

                // XXX other cases
            }

            final ChatFormatting color = ChatFormatting.getByName(mid);
            if (color != null) {
                final MutableComponent postFmtd = post.format(observation);
                postFmtd.withStyle(color);
                return postFmtd;
            }

            return Component.literal("{")
                    .append(pre.format(observation))
                    .append(Component.literal("$" + mid + "$"))
                    .append(post.format(observation))
                    .append(Component.literal("}"));
        }
    }
}
