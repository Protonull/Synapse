package gjum.minecraft.civ.synapse.mod.gui;

import static gjum.minecraft.civ.synapse.common.Util.accountsSepRegex;
import static gjum.minecraft.civ.synapse.common.Util.factionsSepRegex;
import static gjum.minecraft.civ.synapse.common.Util.getLastWord;
import static gjum.minecraft.civ.synapse.common.Util.lowerCaseSet;
import static gjum.minecraft.civ.synapse.common.Util.replaceLastWord;
import static gjum.minecraft.civ.synapse.common.Util.sortedUniqListIgnoreCase;
import static gjum.minecraft.civ.synapse.mod.LiteModSynapse.getStandingColor;
import static gjum.minecraft.civ.synapse.mod.gui.MainGui.makeTitleRow;
import static gjum.minecraft.civ.synapse.mod.gui.MainGui.spacer;
import static gjum.minecraft.civ.synapse.mod.gui.MainGui.stretchX;
import static gjum.minecraft.gui.Label.Alignment.ALIGN_LEFT;
import static gjum.minecraft.gui.Label.Alignment.ALIGN_RIGHT;
import static gjum.minecraft.gui.Vec2.Direction.COLUMN;
import static gjum.minecraft.gui.Vec2.Direction.ROW;

import gjum.minecraft.civ.synapse.mod.LiteModSynapse;
import gjum.minecraft.civ.synapse.mod.Person;
import gjum.minecraft.civ.synapse.mod.PersonsRegistry;
import gjum.minecraft.civ.synapse.mod.Standing;
import gjum.minecraft.gui.Button;
import gjum.minecraft.gui.CycleButton;
import gjum.minecraft.gui.FlexListLayout;
import gjum.minecraft.gui.GuiElement;
import gjum.minecraft.gui.GuiRoot;
import gjum.minecraft.gui.Label;
import gjum.minecraft.gui.ScrollBox;
import gjum.minecraft.gui.Spacer;
import gjum.minecraft.gui.TextField;
import gjum.minecraft.gui.Tooltip;
import gjum.minecraft.gui.Vec2;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.Tuple;
import net.minecraft.util.text.TextFormatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PersonGui extends GuiRoot {
	private static final int maxAccountSuggestions = 5;

	@NotNull
	private final Person person;
	@NotNull
	private String newName;
	@NotNull
	private String newAccounts;
	@NotNull
	private String newFactions = "";
	@Nullable
	private FlexListLayout suggestAccountsRow = null;

	public PersonGui(GuiScreen parent, @NotNull Person person) {
		super(parent);
		this.person = person;
		newName = person.getName();
		newAccounts = String.join(" ", sortedUniqListIgnoreCase(person.getAccounts()));
	}

	@Override
	public GuiElement build() {
		final LiteModSynapse mod = LiteModSynapse.instance;
		final PersonsRegistry personsRegistry = mod.getPersonsRegistry();
		if (personsRegistry == null) {
			mod.showGuiAndRemember(parentScreen);
			return null;
		}

		final FlexListLayout titleRow = makeTitleRow(this, "Person: " + person.getName());

		final FlexListLayout personRow = new FlexListLayout(ROW);
		final String anyAccount = person.getAccounts().isEmpty() ? "" : person.getAccounts().iterator().next();
		final Button changeNameBtn = new Button(() -> {
			if (newName.isEmpty()) newName = anyAccount;
			if (!newName.isEmpty()) {
				person.setName(newName);
				rebuild();
			}
		}, "Change nickname");
		changeNameBtn.setEnabled(!person.getName().equals(newName));
		personRow.add(new TextField(newNameRaw -> {
			newName = newNameRaw.trim();
			final Person conflictingPerson = personsRegistry.personByName(newName);
			final boolean noConflict = conflictingPerson == null || conflictingPerson == person;
			final boolean valid = noConflict;
			changeNameBtn.setEnabled(valid);
			return valid;
		}, newName, anyAccount)
				.setEnterHandler(changeNameBtn.clickHandler)
				.setWeight(stretchX));
		personRow.add(changeNameBtn);
		personRow.add(new Spacer().setWeight(stretchX));
		personRow.add(new Spacer(spacer));
		personRow.add(new Button(() -> {
			// TODO confirm deletion
			personsRegistry.remove(person);
			mod.showGuiAndRemember(parentScreen);
		}, "Delete person"));

		final Standing currentStanding = mod.serverConfig.getStanding(person);
		final FlexListLayout standingRow = new FlexListLayout(ROW);
		standingRow.add(new Label("Set standing: ", ALIGN_RIGHT));
		for (Standing standing : Standing.values()) {
			if (standing == Standing.FOCUS) continue;
			final Button btn = new Button(() -> {
				mod.serverConfig.setPersonStanding(person, standing);
				rebuild();
			}, standing.name().toLowerCase());
			btn.setEnabled(standing != currentStanding);
			final Tuple<Collection<String>, String> changes = mod.serverConfig.simulateSetPersonStanding(person, standing);
			final Collection<String> removedFactions = changes.getFirst();
			if (removedFactions.isEmpty()) {
				standingRow.add(btn);
			} else {
				final String tooltip = "Remove " + person.getName() + " from: " + String.join(", ", sortedUniqListIgnoreCase(removedFactions));
				standingRow.add(new Tooltip(tooltip, btn));
			}
		}

		standingRow.add(new Spacer(spacer));
		standingRow.add(new CycleButton<>(newValue -> {
			if ("unfocus".equals(newValue)) {
				// "focus" was pressed
				mod.announceFocusedAccounts(person.getAccounts());
			} else {
				// "unfocus" was pressed
				mod.setFocusedAccountNames(Collections.emptyList());
			}
			rebuild();
		}, currentStanding != Standing.FOCUS ? "focus" : "unfocus",
				"focus", "unfocus"));

		suggestAccountsRow = new FlexListLayout(ROW);
		addAccountSuggestions();

		final Button saveAccountsBtn = new Button(() -> {
			person.setAccounts(Arrays.asList(newAccounts.split(accountsSepRegex)));
			rebuild();
		}, "Set accounts");
		final TextField accountsField = new TextField(text -> {
			final String[] split = text.split(accountsSepRegex);
			final boolean valid = Arrays.stream(split).allMatch(word ->
					word.matches("[A-Za-z0-9_]{3,}"));
			newAccounts = text.trim();
			addAccountSuggestions();
			return valid;
		}, newAccounts, "Accounts");
		accountsField.setWeight(stretchX);
		accountsField.setFocused(true);
		accountsField.setEnterHandler(saveAccountsBtn.clickHandler);

		final FlexListLayout accountsRow = new FlexListLayout(ROW);
		accountsRow.add(accountsField);
		accountsRow.add(saveAccountsBtn);

		final FlexListLayout factionsRow = new FlexListLayout(ROW);
		final Button addFactionsBtn = new Button(() -> {
			for (String faction : newFactions.split(factionsSepRegex)) {
				person.addFaction(faction);
			}
			newFactions = "";
			rebuild();
		}, "Add factions");
		factionsRow.add(new TextField(newFactions -> {
			this.newFactions = newFactions;
			return true;
		}, newFactions, "Add person to factions")
				.setEnterHandler(addFactionsBtn.clickHandler)
				.setWeight(stretchX));
		factionsRow.add(addFactionsBtn);
		// final FlexListLayout factionsSuggestionsRow = new FlexListLayout(ROW); // XXX buttons to autocomplete faction that matches last word in textField

		// XXX use TableLayout to list factions
		final FlexListLayout factionsList = new FlexListLayout(COLUMN);
		person.getFactions().stream().sorted(Comparator.comparing(String::toLowerCase)).forEachOrdered(faction -> {
			final FlexListLayout row = new FlexListLayout(ROW);

			final Button removeBtn = new Button(() -> {
				person.removeFaction(faction);
				rebuild();
			}, "X");
			final String tooltip = "Remove " + person.getName() + " from " + faction;
			row.add(new Tooltip(tooltip, removeBtn));
			row.add(new Spacer(spacer));

			final Label factionLabel = new Label(faction, ALIGN_LEFT);
			factionLabel.setColor(getStandingColor(mod.serverConfig.getFactionStanding(faction)));
			factionLabel.setClickHandler(() -> mod.showGuiAndRemember(new FactionGui(this, faction)));
			row.add(factionLabel);

			final Collection<Person> members = personsRegistry.personsInFaction(faction);
			members.stream()
					.sorted(Comparator.comparing(mod.serverConfig::getStanding))
					.limit(5)
					.forEachOrdered(member -> {
						final Label memberLabel = new Label(member.getName(), ALIGN_LEFT);
						memberLabel.setColor(getStandingColor(mod.serverConfig.getStanding(member)));
						memberLabel.setClickHandler(() -> mod.showGuiAndRemember(new PersonGui(this, member)));
						row.add(new Spacer(spacer));
						row.add(memberLabel);
					});
			final int notListedMembers = members.size() - 5;
			if (notListedMembers > 0) {
				row.add(new Spacer(spacer));
				row.add(new Label("+" + notListedMembers, ALIGN_LEFT));
			}

			factionsList.add(row);
			factionsList.add(new Spacer(spacer));
		});
		factionsList.add(new Spacer());

		final FlexListLayout content = new FlexListLayout(COLUMN);
		content.add(new Spacer(spacer));
		content.add(titleRow);
		content.add(new Spacer(spacer));
		content.add(personRow);
		content.add(new Spacer(spacer));
		content.add(standingRow);
		content.add(new Spacer(spacer));
		content.add(accountsRow);
		content.add(new Spacer(spacer));
		content.add(suggestAccountsRow);
		content.add(new Spacer(spacer));
		content.add(factionsRow);
		content.add(new Spacer(spacer));
		content.add(new ScrollBox(factionsList)
				.setWeight(new Vec2(1, 1)));
		content.add(new Spacer(spacer));

		return new FlexListLayout(ROW)
				.add(new Spacer(spacer))
				.add(content)
				.add(new Spacer(spacer));
	}

	private void addAccountSuggestions() {
		if (suggestAccountsRow == null) return;
		suggestAccountsRow.clear();
		final String lastWord = getLastWord(newAccounts).trim();
		if (lastWord.isEmpty()) return;
		suggestAccountsRow.add(new Label("Suggestions: ", ALIGN_LEFT));
		final LiteModSynapse mod = LiteModSynapse.instance;
		final Set<String> newAccountsSet = lowerCaseSet(Arrays.asList(newAccounts.split(accountsSepRegex)));
		final Stream<String> suggestions = mod
				.accountsConfig.findSimilarScoredStream(lastWord)
				.map(Tuple::getFirst)
				.filter(a -> !newAccountsSet.contains(a.toLowerCase()))
				.limit(maxAccountSuggestions);
		suggestions.forEachOrdered(account -> {
			String text = account;
			final PersonsRegistry personsRegistry = mod.getPersonsRegistry();
			if (personsRegistry != null) {
				final Person person = personsRegistry.personByAccountName(account);
				if (person != null) {
					final TextFormatting color = mod.config.getStandingColor(
							mod.serverConfig.getStanding(person));
					text = account + color + " (" + person.getName() + ")";
				}
			}
			final Button btn = new Button(() -> {
				newAccounts = replaceLastWord(newAccounts, account);
				rebuild();
			}, text);
			btn.setWeight(new Vec2(1, 0));
			btn.setEnabled(account != null
					&& !account.isEmpty());
			suggestAccountsRow.add(btn);
		});
	}
}
