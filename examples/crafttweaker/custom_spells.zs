// Custom Hex action examples for Hex Casting Legacy 1.12.2.
// Copy this file to the instance's scripts/ directory after installing
// CraftTweaker. The angle strings use w/e/d/s/a/q keyboard notation.
// The start direction is one of NORTH_EAST, EAST, SOUTH_EAST, SOUTH_WEST,
// WEST, or NORTH_WEST.

import mods.hexcasting.CustomSpells;
import mods.hexcasting.Iotas;

// ---------------------------------------------------------------------------
// Basic stack input/output
// ---------------------------------------------------------------------------
// Input: one number. Output: that number multiplied by two.
// Test stack: 21.0 -> this action -> 42.0.
CustomSpells.register(
    "example:double_number",
    "EAST",
    "qwedasqwedasqwe",
    function(stack, environment, image) {
        var value = stack.pop();
        if (!value.isNumber()) {
            image.mishap("example.hexcasting.mishap.expected_number");
        }
        stack.push(Iotas.number(value.number * 2.0));
    }
);

// Input order at the action is: target Entity Iota, then damage number.
// Example sequence in the staff: get_caster, entity_pos/eye, get_caster,
// get_entity_look, raycast/entity, 5.0, then this action.
// Media cost is damage squared, with 10000 media per squared damage point.
CustomSpells.register(
    "example:damage_target",
    "EAST",
    "qweqweqwe",
    function(stack, environment, image) {
        stack.requireSize(2);
        var damage = stack.popNumber();
        var target = stack.pop();
        if (!target.isEntity()) {
            image.mishap("hexcasting.error.invalid_iota",
                "damage_target expects an Entity Iota");
        }
        if (damage != damage || damage <= 0.0 || damage > 100.0) {
            image.mishap("hexcasting.error.invalid_value",
                "damage must be between 0 and 100");
        }

        var mediaCost = (damage * damage * 10000.0) as long;
        environment.consumeMedia(mediaCost);
        environment.damageEntity(target, damage as float);
    }
);

// Demonstrates the read-only Iota type checks and accessors. It accepts one
// Iota, records its type/display/truthiness, and returns an equivalent Iota.
CustomSpells.register(
    "example:inspect_iota",
    "NORTH_EAST",
    "sawqdesawqde",
    function(stack, environment, image) {
        stack.requireSize(1);
        var value = stack.pop();
        image.setUserData({
            type: value.type,
            display: value.display,
            truthy: value.truthy,
            toleratesSelf: value.tolerates(value)
        });

        if (value.isNumber()) {
            stack.push(Iotas.number(value.number));
        } else if (value.isBoolean()) {
            stack.push(Iotas.booleanValue(value.booleanValue));
        } else if (value.isVector()) {
            stack.push(Iotas.vector(value.x, value.y, value.z));
        } else if (value.isItem()) {
            stack.push(Iotas.item(value.item));
        } else if (value.isBlock()) {
            stack.push(Iotas.block(value.blockState));
        } else if (value.isEntity()) {
            stack.push(Iotas.entity(value.entity));
        } else if (value.isList()) {
            stack.push(Iotas.list(value.getList()));
        } else if (value.isContinuation()) {
            stack.push(Iotas.continuation(value.getContinuation()));
        } else {
            // Pattern, null, and garbage Iotas can be passed through without
            // reconstructing them from a field value.
            stack.push(value);
        }
    }
);

// Uses the boolean-specific stack pop helper.
CustomSpells.register(
    "example:boolean_echo",
    "SOUTH_EAST",
    "qwsedqwsed",
    function(stack, environment, image) {
        stack.push(Iotas.booleanValue(!stack.popBoolean()));
    }
);

// ---------------------------------------------------------------------------
// Environment and image state
// ---------------------------------------------------------------------------
// Stores the active action context in the VM user data and returns a small
// list, demonstrating Zen getters and IData persistence in one action.
CustomSpells.register(
    "example:context_report",
    "NORTH",
    "wqedsaqwedsa",
    function(stack, environment, image) {
        image.setUserData({
            action: environment.actionId,
            hand: environment.hand,
            dimension: environment.dimension,
            circle: environment.circle,
            pattern: environment.patternSignature,
            angles: environment.patternAngles
        });
        stack.push(Iotas.list([
            Iotas.booleanValue(environment.circle),
            Iotas.number(environment.dimension as double),
            Iotas.number(environment.availableMedia as double)
        ]));
    }
);

// Converts a serialized Entity Iota back to the current loaded world before
// putting it back on the stack. This is useful after a second drawing or a
// world reload, where a stale Java entity reference must not be used.
CustomSpells.register(
    "example:resolve_entity",
    "SOUTH_EAST",
    "wedqsaqwedq",
    function(stack, environment, image) {
        var target = stack.pop();
        if (!target.isEntity()) {
            image.mishap("hexcasting.error.invalid_iota",
                "resolve_entity expects an Entity Iota");
        }
        var resolved = environment.resolveEntity(target);
        stack.push(Iotas.entity(resolved));
    }
);

// ---------------------------------------------------------------------------
// Iota constructors and NBT round trip
// ---------------------------------------------------------------------------
// Demonstrates number, boolean, vector, item, block, pattern, list, null,
// garbage, entity, toNBT, and fromNBT. The entity is kept outside the encoded
// list so this example does not depend on an entity remaining loaded later.
CustomSpells.register(
    "example:iota_factory",
    "SOUTH_WEST",
    "asdwqedasdwq",
    function(stack, environment, image) {
        var entity = Iotas.entity(environment.caster);
        var payload = Iotas.list([
            Iotas.number(3.0),
            Iotas.booleanValue(true),
            Iotas.vector(1.0, 2.0, 3.0),
            Iotas.item(<minecraft:diamond>),
            Iotas.block(<blockstate:minecraft:stone>),
            Iotas.pattern("EAST", "qwe"),
            Iotas.nullValue(),
            Iotas.garbage()
        ]);
        var encoded = Iotas.toNBT(payload);
        var decoded = Iotas.fromNBT(encoded);
        stack.push(decoded);
        stack.push(entity);
    }
);

// Evaluates the existing double_number action through runNested. The nested
// VM uses the same casting stack and keeps normal rollback semantics.
CustomSpells.register(
    "example:nested_double",
    "WEST",
    "dsawqedsaqwe",
    function(stack, environment, image) {
        stack.push(Iotas.number(21.0));
        var nested = image.runNested(Iotas.list([
            Iotas.pattern("EAST", "qwedasqwedasqwe")
        ]));
        stack.push(Iotas.number(nested.popNumber()));
    }
);

// Captures the pending continuation and exposes it as a normal Iota. The
// result can later be fed to image.invokeContinuation by another custom action.
CustomSpells.register(
    "example:capture_continuation",
    "NORTH_WEST",
    "qsaedwqsaedw",
    function(stack, environment, image) {
        stack.push(image.captureContinuation());
    }
);

// Enqueues a known custom pattern. The pushed number is consumed by the
// queued double_number action and the resulting number remains on the stack.
CustomSpells.register(
    "example:enqueue_double",
    "EAST",
    "edqwasdqwasd",
    function(stack, environment, image) {
        stack.push(Iotas.number(10.0));
        image.enqueuePattern(Iotas.pattern("EAST", "qwedasqwedasqwe"));
    }
);

// enqueueFirst puts the pattern at the front of the pending continuation.
CustomSpells.register(
    "example:enqueue_first_double",
    "WEST",
    "wedsawedsaw",
    function(stack, environment, image) {
        stack.push(Iotas.number(12.0));
        image.enqueueFirst(Iotas.pattern("EAST", "qwedasqwedasqwe"));
    }
);

// Invoke a continuation supplied by the surrounding spell. This is useful
// with example:capture_continuation; it intentionally requires a continuation
// Iota on the stack and otherwise produces the normal invalid-Iota Mishap.
CustomSpells.register(
    "example:invoke_continuation",
    "NORTH_WEST",
    "asqwedasqwed",
    function(stack, environment, image) {
        image.invokeContinuation(stack.pop());
    }
);

// ---------------------------------------------------------------------------
// Stack editing and media
// ---------------------------------------------------------------------------
// Exercises snapshot/restore, peek, get, replaceFromTop, set, local/ravenmind,
// and the size/empty properties without requiring any input Iotas.
CustomSpells.register(
    "example:stack_state",
    "NORTH",
    "qweasdqweasd",
    function(stack, environment, image) {
        var saved = stack.snapshot();
        var oldLocal = stack.local;
        stack.local = Iotas.number(99.0);
        var localValue = stack.ravenmind;
        stack.ravenmind = oldLocal;

        stack.push(Iotas.number(1.0));
        var topBefore = stack.peek();
        var first = stack.get(stack.size() - 1);
        stack.replaceFromTop(0, Iotas.number(2.0));
        stack.set(stack.size() - 1, Iotas.number(3.0));
        var sizeAfterEdits = stack.size();
        stack.restore(saved);

        stack.push(Iotas.list([
            topBefore,
            first,
            localValue,
            Iotas.number(sizeAfterEdits as double)
        ]));
    }
);

// Uses the image-side media helper. A failed consumeMedia call becomes the
// normal insufficient-media Mishap and the VM transaction rolls back.
CustomSpells.register(
    "example:consume_media",
    "SOUTH_EAST",
    "qwedqwedqwed",
    function(stack, environment, image) {
        image.consumeMedia(10000);
        stack.push(Iotas.number(10000.0));
    }
);

// Great actions use the same seed-based per-world pattern table as built-in
// great spells and require enlightenment before they can execute.
CustomSpells.registerGreat(
    "example:great_number",
    "NORTH_EAST",
    "wqedsaqwedsaqwe",
    function(stack, environment, image) {
        stack.push(Iotas.number(42.0));
    }
);

// registerWithOptions can make an action execute while a parenthesis is being
// captured. This action is intentionally simple so it is safe to test inside
// a parenthesized program.
CustomSpells.registerWithOptions(
    "example:parenthesized_number",
    "SOUTH_WEST",
    "qweasqweasqwe",
    false,
    true,
    function(stack, environment, image) {
        stack.push(Iotas.number(7.0));
    }
);

// ---------------------------------------------------------------------------
// Failure and VM-control examples
// ---------------------------------------------------------------------------
// This action intentionally fails. It is useful for checking localized
// Mishaps, stack effects, media rollback, and the lastMishapKey getter in a
// wrapper action. Do not use it in a production spell.
CustomSpells.register(
    "example:forced_mishap",
    "WEST",
    "qazwsxedcrf",
    function(stack, environment, image) {
        image.mishap("example.hexcasting.mishap.forced",
            "This is an intentional test failure");
    }
);

// halt stops pending continuation work after this action completes.
CustomSpells.register(
    "example:halt",
    "NORTH_EAST",
    "edcwaqedcwaq",
    function(stack, environment, image) {
        stack.push(Iotas.number(1.0));
        image.halt();
    }
);

// Demonstrates the Mishap stack-effect helpers. The resulting garbage Iotas
// are intentional and make this action useful when testing error visuals.
CustomSpells.register(
    "example:garbage_effects",
    "SOUTH_WEST",
    "qedwsqedwsq",
    function(stack, environment, image) {
        stack.clearAndPushGarbage();
        stack.pushGarbage(2);
    }
);

// The following calls are available for more specialized scripts. They are
// shown as comments because they deliberately change VM control state or need
// a continuation/parenthesis supplied by the surrounding spell:
// image.enqueue(Iotas.pattern("EAST", "qwedasqwedasqwe"));
// image.enqueueFirst(Iotas.pattern("EAST", "qwedasqwedasqwe"));
// image.invokeContinuation(stack.pop());
// image.setEscapeNext();
// image.resetEscape();
// image.resetMetaState();
// image.openParen();
// image.openParens(2);
// image.closeParen();
// image.closeAllParens();
// image.readIntoParen();
// image.mishap("example.hexcasting.mishap.forced");
// var key = image.lastMishapKey;
// stack.clear();
