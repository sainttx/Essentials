package org.mcess.essentials.metrics;

import org.mcess.essentials.register.payment.Method;
import org.mcess.essentials.register.payment.methods.VaultEco;
import org.mcess.essentials.signs.EssentialsSign;
import java.util.Locale;
import java.util.logging.Level;
import net.ess3.api.IEssentials;
import org.bukkit.configuration.ConfigurationSection;


public class MetricsStarter implements Runnable
{
	private final IEssentials ess;
	private transient Boolean start;


	private enum Modules
	{
		Essentials,
		EssentialsAntiBuild,
		EssentialsAntiCheat,
		EssentialsChat,
		EssentialsSpawn,
		EssentialsProtect,
		EssentialsGeoIP
	};

	public MetricsStarter(final IEssentials plugin)
	{
		ess = plugin;
		try
		{

			final Metrics metrics = new Metrics(ess);
			ess.setMetrics(metrics);

			if (!metrics.isOptOut())
			{
				if (ess.getSettings().isMetricsEnabled())
				{
					start = true;
				}
				else
				{
					ess.getLogger().info("This plugin collects minimal statistic data and sends it to http://metrics.essentials3.net.");
					ess.getLogger().info("You can opt out, disabling metrics for all plugins, by running /essentials opt-out");
					ess.getLogger().info("This will start 5 minutes after the first admin/op joins.");
					start = false;
				}
				return;
			}
		}
		catch (Exception ex)
		{
			metricsError(ex);
		}
	}

	@Override
	public void run()
	{
		try
		{
			final Metrics metrics = ess.getMetrics();

			final Metrics.Graph moduleGraph = metrics.createGraph("Modules Used");
			for (Modules module : Modules.values())
			{
				final String moduleName = module.toString();
				if (ess.getServer().getPluginManager().isPluginEnabled(moduleName))
				{
					moduleGraph.addPlotter(new SimplePlotter(moduleName));
				}
			}

			final Metrics.Graph localeGraph = metrics.createGraph("Locale");
			localeGraph.addPlotter(new SimplePlotter(ess.getI18n().getCurrentLocale().getDisplayLanguage(Locale.ENGLISH)));

			final Metrics.Graph featureGraph = metrics.createGraph("Features");
			featureGraph.addPlotter(new Metrics.Plotter("Unique Accounts")
			{
				@Override
				public int getValue()
				{
					return ess.getUserMap().getUniqueUsers();
				}
			});
			featureGraph.addPlotter(new Metrics.Plotter("Jails")
			{
				@Override
				public int getValue()
				{
					return ess.getJails().getCount();
				}
			});
			featureGraph.addPlotter(new Metrics.Plotter("Kits")
			{
				@Override
				public int getValue()
				{
					ConfigurationSection kits = ess.getSettings().getKits();
					if (kits == null)
					{
						return 0;
					}
					return kits.getKeys(false).size();
				}
			});
			featureGraph.addPlotter(new Metrics.Plotter("Warps")
			{
				@Override
				public int getValue()
				{
					return ess.getWarps().getCount();
				}
			});

			final Metrics.Graph enabledGraph = metrics.createGraph("EnabledFeatures");
			enabledGraph.addPlotter(new SimplePlotter("Total"));
			if (ess.getJails().getCount() > 0)
			{
				enabledGraph.addPlotter(new SimplePlotter("Jails"));
			}
			if (ess.getSettings().getKits() != null && ess.getSettings().getKits().getKeys(false).size() > 0)
			{
				enabledGraph.addPlotter(new SimplePlotter("Kits"));
			}
			if (ess.getWarps().getCount() > 0)
			{
				enabledGraph.addPlotter(new SimplePlotter("Warps"));
			}
			if (ess.getSettings().getTeleportCooldown() > 0)
			{
				enabledGraph.addPlotter(new SimplePlotter("TeleportCooldown"));
			}
			if (ess.getSettings().getTeleportDelay() > 0)
			{
				enabledGraph.addPlotter(new SimplePlotter("TeleportDelay"));
			}
			if (!ess.getSettings().areSignsDisabled())
			{
				enabledGraph.addPlotter(new SimplePlotter("Signs"));
			}
			if (ess.getSettings().getAutoAfk() > 0)
			{
				enabledGraph.addPlotter(new SimplePlotter("AutoAFK"));
			}
			if (ess.getSettings().changePlayerListName())
			{
				enabledGraph.addPlotter(new SimplePlotter("PlayerListName"));
			}
			if (ess.getSettings().getOperatorColor() != null)
			{
				enabledGraph.addPlotter(new SimplePlotter("OpColour"));
			}
			if (ess.getSettings().changeDisplayName())
			{
				enabledGraph.addPlotter(new SimplePlotter("DisplayName"));
			}
			if (ess.getSettings().getChatRadius() >= 1)
			{
				enabledGraph.addPlotter(new SimplePlotter("LocalChat"));
			}

			final Metrics.Graph depGraph = metrics.createGraph("Dependencies");
			final Method method = ess.getPaymentMethod().getMethod();
			if (method != null)
			{
				String version;
				if (method instanceof VaultEco)
				{
					version = ((VaultEco)method).getEconomy();
				}
				else
				{
					version = method.getVersion();
					final int dashPosition = version.indexOf('-');
					if (dashPosition > 0)
					{
						version = version.substring(0, dashPosition);
					}
				}
				depGraph.addPlotter(new SimplePlotter(method.getName() + " " + version));
			}
			depGraph.addPlotter(new SimplePlotter(ess.getPermissionsHandler().getName()));

			final Metrics.Graph signGraph = metrics.createGraph("Signs");
			for (EssentialsSign sign : ess.getSettings().enabledSigns())
			{
				signGraph.addPlotter(new SimplePlotter(sign.getName()));
			}

			metrics.start();

		}
		catch (Exception ex)
		{
			metricsError(ex);
		}
	}

	public void metricsError(final Exception ex)
	{
		if (ess.getSettings().isDebug())
		{
			ess.getLogger().log(Level.INFO, "[Metrics] " + ex.getMessage(), ex);
		}
		else
		{
			ess.getLogger().log(Level.INFO, "[Metrics] " + ex.getMessage());
		}
	}

	public Boolean getStart()
	{
		return start;
	}


	private class SimplePlotter extends Metrics.Plotter
	{
		public SimplePlotter(final String name)
		{
			super(name);
		}

		@Override
		public int getValue()
		{
			return 1;
		}
	}
}